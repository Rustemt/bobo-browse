package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.filter.AndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.query.FastMatchAllDocsQuery;
import com.browseengine.bobo.search.BoboSearcher2;
import com.browseengine.bobo.search.FacetHitCollector;
import com.browseengine.bobo.search.InternalBrowseHitCollector;

/**
 * This class implements the browsing functionality.
 */
public class BoboBrowser extends BoboSearcher2 implements Browsable
{
  private final BoboIndexReader           _reader;
  private final Map<String, FacetHandler> _runtimeFacetHandlerMap;

  private static Logger                   logger = Logger.getLogger(BoboBrowser.class);

  public BoboIndexReader getIndexReader()
  {
    return _reader;
  }

  /**
   * Constructor.
   * 
   * @param reader
   *          A bobo reader instance
   */
  public BoboBrowser(BoboIndexReader reader)
  {
    super(reader);
    _reader = reader;
    _runtimeFacetHandlerMap = new HashMap<String, FacetHandler>();
  }
  
  private static boolean isNoQueryNoFilter(BrowseRequest req)
  {
    Query q = req.getQuery();
    Filter filter = req.getFilter();
    return ((q == null || q instanceof MatchAllDocsQuery || q instanceof FastMatchAllDocsQuery) && filter == null); 
  }

  /**
   * Sets runtime facet handler. If has the same name as a preload handler, for the
   * duration of this browser, this one will be used.
   * 
   * @param facetHandler
   *          Runtime facet handler
   */
  public void setFacetHandler(FacetHandler facetHandler) throws IOException
  {
	Set<String> dependsOn = facetHandler.getDependsOn();
	BoboIndexReader reader = (BoboIndexReader) getIndexReader();
	if (dependsOn.size() > 0)
	{
		Iterator<String> iter = dependsOn.iterator();
		while(iter.hasNext())
		{
			String fn = iter.next();
			FacetHandler f = _runtimeFacetHandlerMap.get(fn);
			if (f == null)
			{
				f = reader.getFacetHandler(fn);
			}
			if (f==null)
			{
				throw new IOException("depended on facet handler: "+fn+", but is not found");
			}
			facetHandler.putDependedFacetHandler(f);
		}
	}
    facetHandler.load(reader);
    _runtimeFacetHandlerMap.put(facetHandler.getName(), facetHandler);
  }

  /**
   * Gets a defined facet handler
   * 
   * @param name
   *          facet name
   * @return a facet handler
   */
  public FacetHandler getFacetHandler(String name)
  {
    FacetHandler handler = _runtimeFacetHandlerMap.get(name);
    if (handler == null)
    {
      return _reader.getFacetHandler(name);
    }
    else
    {
      return handler;
    }
  }

  /**
   * Gets a set of facet names
   * 
   * @return set of facet names
   */
  public Set<String> getFacetNames()
  {
    Set<String> runtimeFacetNames = _runtimeFacetHandlerMap.keySet();
    Set<String> installedFacetNames = _reader.getFacetNames();
    if (runtimeFacetNames.size() > 0)
    {
      Set<String> names = new HashSet<String>();
      names.addAll(_reader.getFacetNames());
      names.addAll(_runtimeFacetHandlerMap.keySet());
      return names;
    }
    else
    {
      return installedFacetNames;
    }
  }

  /**
   * browses the index.
   * 
   * @param req
   *          browse request
   * @param hitCollector
   *          collector for the hits
   * @param facetMap map to gather facet data
   */
  public void browse(BrowseRequest req,
                     HitCollector hitCollector,
                     Map<String, FacetAccessible> facetMap) throws BrowseException
  {
    if (_reader == null)
      return;

    Set<String> fields = getFacetNames();

    LinkedList<Filter> preFilterList = new LinkedList<Filter>();
    List<FacetHitCollector> facetHitCollectorList = new LinkedList<FacetHitCollector>();
    List<FacetCountCollector> countAllCollectorList = new LinkedList<FacetCountCollector>();
    
    Filter baseFilter = req.getFilter();
    if (baseFilter != null)
    {
      preFilterList.add(baseFilter);
    }

    int selCount = req.getSelectionCount();
    boolean isNoQueryNoFilter = isNoQueryNoFilter(req);

    boolean isDefaultSearch = isNoQueryNoFilter && selCount == 0;
    try
    {
      
      for (String name : fields)
      {
        BrowseSelection sel = req.getSelection(name);
        FacetSpec ospec = req.getFacetSpec(name);

        FacetHandler handler = getFacetHandler(name);
        
        if (handler == null){
        	logger.warn("facet handler: "+name+" is not defined, ignored.");
        	continue;
        }
        
        FacetHitCollector facetHitCollector = null;

        RandomAccessFilter filter = null;
        if (sel != null)
        {
          filter = handler.buildFilter(sel);
        }

        if (ospec == null)
        {
          if (filter != null)
          {
            preFilterList.add(filter);
          }
        }
        else
        {
          if (isDefaultSearch)
          {
            countAllCollectorList.add(handler.getFacetCountCollector(sel, ospec));
          }
          else
          {
            facetHitCollector = new FacetHitCollector();
            facetHitCollector._facetCountCollector = handler.getFacetCountCollector(sel, ospec);
            facetHitCollector.facetHandler = handler;
            if (ospec.isExpandSelection())
            {
              if (isNoQueryNoFilter && sel!=null && selCount == 1)
              {
                facetHitCollector = null; // don't post collect
                countAllCollectorList.add(handler.getFacetCountCollector(sel, ospec));
                if (filter != null)
                {
                  preFilterList.add(filter);
                }
              }
              else
              {
                if (filter != null)
                {
                  RandomAccessDocIdSet docset = filter.getRandomAccessDocIdSet(_reader);
                  facetHitCollector._postDocIDSetIterator = docset.iterator();
                  facetHitCollector._docidSet = docset;
                }
              }
            }
            else
            {
              if (filter != null)
              {
                preFilterList.add(filter);
              }
            }
          }
        }
        if (facetHitCollector != null)
        {
          facetHitCollectorList.add(facetHitCollector);
        }
      }

      Filter finalFilter = null;
      if (preFilterList.size() > 0)
      {
        if (preFilterList.size() == 1)
        {
          finalFilter = preFilterList.getFirst();
        }
        else
        {
          finalFilter = new AndFilter(preFilterList);
        }
      }

      setFacetHitCollectorList(facetHitCollectorList);

      Query q = req.getQuery();
      if (q == null || q instanceof MatchAllDocsQuery)
      {
        q = _reader.getFastMatchAllDocsQuery();
      }
      
      try
      {
        search(q, finalFilter, hitCollector);
      }
      finally
      {
        for (FacetHitCollector facetCollector : facetHitCollectorList)
        {
          String name = facetCollector._facetCountCollector.getName();
          facetMap.put(name, facetCollector._facetCountCollector);
        }
        for (FacetCountCollector facetCountCollector : countAllCollectorList)
        {
          facetCountCollector.collectAll();
          facetMap.put(facetCountCollector.getName(), facetCountCollector);
        }
      }
    }
    catch (IOException ioe)
    {
      throw new BrowseException(ioe.getMessage(), ioe);
    }
  }

  /**
   * browses the index.
   * 
   * @param req
   *          browse request
   * @return browse result
   */
  public BrowseResult browse(BrowseRequest req) throws BrowseException
  {
    if (_reader == null)
      return new BrowseResult();

    final BrowseResult result = new BrowseResult();

    long start = System.currentTimeMillis();

    int offset = req.getOffset();
    int count = req.getCount();
    
    if (offset<0 || count<0){
    	throw new IllegalArgumentException("both offset and count must be > 0: "+offset+"/"+count);
    }
    TopDocsSortedHitCollector myHC = getSortedHitCollector(req.getSort(),offset,count,req.isFetchStoredFields());
    Map<String, FacetAccessible> facetCollectors = new HashMap<String, FacetAccessible>();
    browse(req, myHC, facetCollectors);
    BrowseHit[] hits = null;

    try
    {
      hits = myHC.getTopDocs();
    }
    catch (IOException e)
    {
      logger.error(e.getMessage(), e);
      hits = new BrowseHit[0];
    }
    result.setHits(hits);
    result.setNumHits(myHC.getTotalHits());
    result.setTotalDocs(_reader.numDocs());
    result.addAll(facetCollectors);
    long end = System.currentTimeMillis();
    result.setTime(end - start);
    return result;
  }
  
  public Map<String, FacetHandler> getRuntimeFacetHandlerMap()
  {
    return _runtimeFacetHandlerMap;
  }

  public int numDocs()
  {
    return _reader.numDocs();
  }

  @Override
  public Document doc(int docid) throws CorruptIndexException,
      IOException
  {
    Document doc = super.doc(docid);
    for (FacetHandler handler : _runtimeFacetHandlerMap.values())
    {
      String[] vals = handler.getFieldValues(docid);
      for (String val : vals)
      {
        doc.add(new Field(handler.getName(),
                          val,
                          Field.Store.NO,
                          Field.Index.NOT_ANALYZED));
      }
    }
    return doc;
  }

  /**
   * Returns the field data for a given doc.
   * 
   * @param docid
   *          doc
   * @param fieldname
   *          name of the field
   * @return field data
   */
  public String[] getFieldVal(int docid, final String fieldname) throws IOException
  {
    FacetHandler facetHandler = getFacetHandler(fieldname);
    if (facetHandler != null)
    {
      return facetHandler.getFieldValues(docid);
    }
    else
    {
      logger.warn("facet handler: " + fieldname
          + " not defined, looking at stored field.");
      // this is not predefined, so it will be slow
      Document doc = _reader.document(docid, new FieldSelector()
      {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept(String field)
        {
          if (fieldname.equals(field))
          {
            return FieldSelectorResult.LOAD_AND_BREAK;
          }
          else
          {
            return FieldSelectorResult.NO_LOAD;
          }
        }
      });
      return doc.getValues(fieldname);
    }
  }

  public TopDocsSortedHitCollector getSortedHitCollector(SortField[] sort,
                                                         int offset,
                                                         int count,
                                                         boolean fetchStoredFields)
  {
    return new InternalBrowseHitCollector(this, sort, offset, count,fetchStoredFields);
  }

}
