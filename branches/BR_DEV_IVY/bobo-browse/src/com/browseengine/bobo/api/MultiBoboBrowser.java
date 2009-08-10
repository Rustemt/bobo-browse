package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.impl.SortedFieldBrowseHitComparator;
import com.browseengine.bobo.search.MultiTopDocsSortedHitCollector;
import com.browseengine.bobo.util.ListMerger;


/**
 * Provides implementation of Browser for multiple Browser instances
 */
public class MultiBoboBrowser extends MultiSearcher implements Browsable
{
  private static Logger logger = Logger.getLogger(MultiBoboBrowser.class);
  
  /**
   * 
   * @param browsers
   *          Browsers to search on
   * @throws IOException
   */
  public MultiBoboBrowser(Browsable[] browsers) throws IOException
  {
    super(browsers);
  }

  /**
   * Implementation of the browse method using a Lucene HitCollector
   * 
   * @param req
   *          BrowseRequest
   * @param hitCollector
   *          HitCollector for the hits generated during a search
   *          
   */
  public void browse(BrowseRequest req,final HitCollector hitCollector,Map<String, FacetAccessible> facetMap) throws BrowseException
  {
    Browsable[] browsers = getSubBrowsers();
    int[] starts = getStarts();

    Map<String, List<FacetAccessible>> mergedMap = new HashMap<String,List<FacetAccessible>>();
    try
    {

	    Map<String,FacetAccessible> facetColMap = new HashMap<String,FacetAccessible>();
	    for (int i = 0; i < browsers.length; i++)
	    {
	      final int start = starts[i];
	      try
	      {
		      browsers[i].browse(req, new HitCollector()
		      {
		        public void collect(int doc, float score)
		        {
		          hitCollector.collect(doc + start, score);
		        }
		      },facetColMap);
	      }
	      finally
	      {
	    	  Set<Entry<String,FacetAccessible>> entries = facetColMap.entrySet();
	    	  for (Entry<String,FacetAccessible> entry : entries)
	    	  {
	    		  String name = entry.getKey();
	    		  FacetAccessible facetAccessor = entry.getValue();
	    		  List<FacetAccessible> list = mergedMap.get(name);
	    		  if (list == null)
	    		  {
	    			 list = new ArrayList<FacetAccessible>(browsers.length);
	    			 mergedMap.put(name, list);
	    		  }
	    		  list.add(facetAccessor);
	    	  }
	    	  facetColMap.clear();
	      }
	    }
    }
    finally
    {
      Set<Entry<String,List<FacetAccessible>>> entries = mergedMap.entrySet();
  	  for (Entry<String,List<FacetAccessible>> entry : entries)
  	  {
  		  String name = entry.getKey();
  		  FacetHandler handler = getFacetHandler(name);
  		  try
  		  {
  			  List<FacetAccessible> subList = entry.getValue();
  			  if (subList!=null)
  			  {
  			    FacetAccessible merged = handler.merge(req.getFacetSpec(name), subList);
  	  		  	facetMap.put(name, merged);
  			  }
  		  }
  		  catch(Exception e)
  		  {
  			  logger.error(e.getMessage(),e);
  		  }
  	  }
    }
  }

  /**
   * Generate a merged BrowseResult from the given BrowseRequest
   * @param req
   *          BrowseRequest for generating the facets
   * @return BrowseResult of the results of the BrowseRequest
   */
  public BrowseResult browse(BrowseRequest req) throws BrowseException
  {

    long start = System.currentTimeMillis();
    
    TopDocsSortedHitCollector hitCollector = getSortedHitCollector(req.getSort(), req.getOffset(), req.getCount(),req.isFetchStoredFields());

    Map<String, FacetAccessible> mergedMap = new HashMap<String,FacetAccessible>();
    browse(req, hitCollector, mergedMap);
    
    BrowseResult finalResult = new BrowseResult();

    finalResult.setNumHits(hitCollector.getTotalHits());
    finalResult.setTotalDocs(numDocs());
    finalResult.addAll(mergedMap);
    
    BrowseHit[] hits;
    try
    {
      hits = hitCollector.getTopDocs();
    }
    catch (IOException e)
    {
      logger.error(e.getMessage(),e);
      hits=new BrowseHit[0];
    }
    
    finalResult.setHits(hits);
    
    long end = System.currentTimeMillis();
    
    finalResult.setTime(end - start);
    
    return finalResult;
  }
  
  /**
   * Return the values of a field for the given doc
   * 
   */
  public String[] getFieldVal(int docid, final String fieldname) throws IOException
  {
    int i = subSearcher(docid);
    Browsable browser = getSubBrowsers()[i];
    return browser.getFieldVal(subDoc(docid),fieldname);
  }
  
  public int[] getStarts()
  {
    return super.getStarts();
  }

  /**
   * Gets the array of sub-browsers
   * 
   * @return sub-browsers
   * @see MultiSearcher#getSearchables()
   */
  public Browsable[] getSubBrowsers()
  {
    return (Browsable[])getSearchables();
  }

  /**
   * Compare BrowseFacets by their value
   */
  public static class BrowseFacetValueComparator implements Comparator<BrowseFacet>
  {
    public int compare(BrowseFacet o1, BrowseFacet o2)
    {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  /**
   * Gets the sub-browser for a given docid
   * 
   * @param docid
   * @return sub-browser instance
   * @see MultiSearcher#subSearcher(int)
   */
  public Browsable subBrowser(int docid)
  {
    return ((Browsable) (getSubBrowsers()[subSearcher(docid)]));
  }

  @Override
  public void setSimilarity(Similarity similarity)
  {
    super.setSimilarity(similarity);
    for(Browsable subBrowser : getSubBrowsers())
    {
      subBrowser.setSimilarity(similarity);
    }
  }
  
  public int numDocs()
  {
    int count = 0;
    Browsable[] subBrowsers = getSubBrowsers();
    for (Browsable subBrowser : subBrowsers)
    {
      count += subBrowser.numDocs();
    }
    return count;
  }

  public FacetHandler getFacetHandler(String name)
  {
	  Browsable[] subBrowsers = getSubBrowsers();
	  for (Browsable subBrowser : subBrowsers)
	  {
		FacetHandler subHandler = subBrowser.getFacetHandler(name);
		if (subHandler!=null) return subHandler;
	  }
	  return null;
  }
	
  
  public void setFacetHandler(FacetHandler facetHandler) throws IOException
  {
	Browsable[] subBrowsers = getSubBrowsers();
	for (Browsable subBrowser : subBrowsers)
	{
		try {
			subBrowser.setFacetHandler((FacetHandler)facetHandler.clone());
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
	}
  }

  public TopDocsSortedHitCollector getSortedHitCollector(SortField[] sort,
                                                         int offset,
                                                         int count,
                                                         boolean fetchStoredFields)
  {
    return new MultiTopDocsSortedHitCollector(this,sort,offset,count,fetchStoredFields);
  }
}
