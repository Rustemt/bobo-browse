package com.browseengine.bobo.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.TopDocsSortedHitCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.impl.QueryProducer;

public class InternalBrowseHitCollector extends TopDocsSortedHitCollector
{
  public final SortedHitQueue hitQueue;
  public BrowseHit[]                 sortedDocs;
  public SortField[] sortFields;
  private int                         _totalHits;
  private FieldDoc            reusableFD;
  private final BoboIndexReader _reader;
  private final int _offset;
  private final int _count;
  private final BoboBrowser _boboBrowser;
  private final boolean _fetchStoredFields;

  public InternalBrowseHitCollector(BoboBrowser boboBrowser,
                                    SortField[] sort,
                                    int offset,
                                    int count,
                                    boolean fetchStoredFields)
  {
    super();
    _boboBrowser = boboBrowser;
    _reader = boboBrowser.getIndexReader();
    sortFields = QueryProducer.convertSort(sort, _reader);
    _offset = offset;
    _count = count;
    hitQueue = new SortedHitQueue(_boboBrowser, sortFields, offset+count);
    _totalHits = 0;
    _fetchStoredFields = fetchStoredFields;
  }

  @Override
  public void collect(int doc, float score)
  {
      _totalHits++;
      if (reusableFD == null)
        reusableFD = new FieldDoc(doc, score);
      else
      {
        reusableFD.score = score;
        reusableFD.doc = doc;
      }
      reusableFD = (FieldDoc) hitQueue.insertWithOverflow(reusableFD);
  }

  @Override
  public int getTotalHits()
  {
    return _totalHits;
  }
  
  private void fillInRuntimeFacetValues(BrowseHit[] hits)
  {
    Collection<FacetHandler> runtimeFacetHandlers = _boboBrowser.getRuntimeFacetHandlerMap().values();
    for (BrowseHit hit : hits)
    {
      Map<String,String[]> map = hit.getFieldValues();
      int docid = hit.getDocid();
      for (FacetHandler facetHandler : runtimeFacetHandlers)
      {
        String[] values = facetHandler.getFieldValues(docid);
        if (values != null)
        {
          map.put(facetHandler.getName(), values);
        }
      }
    }
  }

  @Override
  public BrowseHit[] getTopDocs() throws IOException
  {
    FieldDoc[] fdocs = hitQueue.getTopDocs(_offset, _count);
    return buildHits(fdocs);
  }
  
  public BrowseHit[] buildHits(FieldDoc[] fdocs) throws IOException
  {
    BrowseHit[] hits = new BrowseHit[fdocs.length];
    int i = 0;
    
    Collection<FacetHandler> facetHandlers= _reader.getFacetHandlerMap().values();
    for (FieldDoc fdoc : fdocs)
    {
      BrowseHit hit=new BrowseHit();
      if (_fetchStoredFields){
         hit.setStoredFields(_reader.document(fdoc.doc));
      }
      Map<String,String[]> map = new HashMap<String,String[]>();
      for (FacetHandler facetHandler : facetHandlers)
      {
          map.put(facetHandler.getName(),facetHandler.getFieldValues(fdoc.doc));
      }
      hit.setFieldValues(map);
      hit.setDocid(fdoc.doc);
      hit.setScore(fdoc.score);
      for (SortField f : sortFields)
      {
        if (f.getType() != SortField.SCORE && f.getType() != SortField.DOC)
        {
          String fieldName = f.getField();
          ScoreDocComparator comparator = hitQueue._comparatorMap.get(fieldName);
          if (comparator!=null){
            hit.addComparable(fieldName, comparator.sortValue(fdoc));
          }
        }
      }
      hits[i++] = hit;
    }
    fillInRuntimeFacetValues(hits);
    return hits;
  }
}
