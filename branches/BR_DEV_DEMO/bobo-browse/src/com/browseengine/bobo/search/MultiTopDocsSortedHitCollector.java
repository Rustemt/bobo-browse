package com.browseengine.bobo.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.MultiBoboBrowser;
import com.browseengine.bobo.api.TopDocsSortedHitCollector;
import com.browseengine.bobo.impl.SortedFieldBrowseHitComparator;
import com.browseengine.bobo.util.ListMerger;

public class MultiTopDocsSortedHitCollector extends TopDocsSortedHitCollector
{
  private static Logger logger = Logger.getLogger(MultiTopDocsSortedHitCollector.class);
  private int _totalCount;
  private final MultiBoboBrowser _multiBrowser;
  private final TopDocsSortedHitCollector[] _subCollectors;
  private final int[] _starts;
  private final int _offset;
  private final int _count;
  private final SortField[] _sort;
  
  
  public MultiTopDocsSortedHitCollector(MultiBoboBrowser multiBrowser,SortField[] sort, Set<String> fieldsToFetch,int offset, int count)
  {
    _sort = sort;
    _offset=offset;
    _count=count;
    _multiBrowser = multiBrowser;
    Browsable[] subBrowsers = _multiBrowser.getSubBrowsers();
    _subCollectors = new TopDocsSortedHitCollector[subBrowsers.length];
    for (int i=0;i<subBrowsers.length;++i)
    {
      _subCollectors[i] = subBrowsers[i].getSortedHitCollector(sort, fieldsToFetch, 0, _offset+_count);
    }
    _starts = _multiBrowser.getStarts();
    _totalCount = 0; 
  }

  @Override
  public void collect(int doc, float score)
  {
    int mapped = _multiBrowser.subDoc(doc);
    int index = _multiBrowser.subSearcher(doc);
    _subCollectors[index].collect(mapped, score);
    _totalCount++;
  }

  @Override
  public BrowseHit[] getTopDocs() throws IOException
  {
    ArrayList<Iterator<BrowseHit>> iteratorList = new ArrayList<Iterator<BrowseHit>>(_subCollectors.length);
    
    for (int i=0;i<_subCollectors.length;++i)
    {
      int base = _starts[i];
      try
      {
        BrowseHit[] subHits = _subCollectors[i].getTopDocs();
        for (BrowseHit hit : subHits)
        {
          hit.setDocid(hit.getDocid()+base);
        }
        iteratorList.add(Arrays.asList(subHits).iterator());
      }
      catch(IOException ioe)
      {
        logger.error(ioe.getMessage(),ioe);
      }
    }
    
    SortField[] sf = _sort;
    if (sf==null || sf.length == 0)
    {
      sf=new SortField[]{SortField.FIELD_SCORE};
    }
    Comparator<BrowseHit> comparator = new SortedFieldBrowseHitComparator(sf);
    
    ArrayList<BrowseHit> mergedList = ListMerger.mergeLists(_offset, _count, iteratorList.toArray(new Iterator[iteratorList.size()]), comparator);
    return mergedList.toArray(new BrowseHit[mergedList.size()]);
  }

  @Override
  public int getTotalHits()
  {
    return _totalCount;
  }

}
