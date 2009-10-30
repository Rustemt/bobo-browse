package com.browseengine.bobo.facets.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;

public class FilterMapFacetHandler extends FacetHandler
{
  protected final Map<String, FacetEntry> _filterMap;
  protected final FacetEntry[] _facetEntries;
  protected BoboIndexReader _reader;
  
  public FilterMapFacetHandler(String name, Map<String, RandomAccessFilter> filterMap)
  {
    super(name);
    _facetEntries = new FacetEntry[filterMap.size()];
    _filterMap = new HashMap<String, FacetEntry>();
    int i=0;
    for(Map.Entry<String, RandomAccessFilter> entry : filterMap.entrySet())
    {
      FacetEntry f = new FacetEntry();
      f.filter = entry.getValue();
      f.value = entry.getKey();
      _facetEntries[i] = f;
      _filterMap.put(f.value, f);
      i++;
    }
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value,Properties props) throws IOException
  {
    return _filterMap.get(value).filter;
  }

  @Override
  public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec fspec)
  {
    return new FilterMapFacetCountCollector();
  }

  @Override
  public String[] getFieldValues(int id)
  {
    List<String> values = new ArrayList<String>();
    for(FacetEntry entry : _facetEntries)
    {
      if(entry.docIdSet.get(id))
        values.add(entry.value);
    }
    return values.size() > 0 ? values.toArray(new String[values.size()]) : null;
  }

  @Override
  public ScoreDocComparator getScoreDocComparator()
  {
    return null;
  }

  @Override
  public void load(BoboIndexReader reader) throws IOException
  {
    _reader = reader;
    for(FacetEntry entry : _facetEntries)
      entry.docIdSet = entry.filter.getRandomAccessDocIdSet(reader);
  }
  
  protected class FacetEntry
  {
    String value;
    RandomAccessFilter filter;
    RandomAccessDocIdSet docIdSet;
  }
  
  protected class FilterMapFacetCountCollector implements FacetCountCollector
  {
    private int[] _counts;
    
    public FilterMapFacetCountCollector()
    {
      _counts = new int[_facetEntries.length];
    }
    
    public int[] getCountDistribution()
    {
      return _counts;
    }

    public void collect(int docid)
    {
      for(int i=0; i<_facetEntries.length; i++)
      {
        if(_facetEntries[i].docIdSet.get(docid))
          _counts[i]++;
      }
    }
    
    public void collectAll()
    {
      throw new IllegalStateException("not supported");
    }

    public List<BrowseFacet> getFacets()
    {
      List<BrowseFacet> facets = new ArrayList<BrowseFacet>();
      for(int i=0; i<_facetEntries.length; i++)
      {
        FacetEntry entry = _facetEntries[i];
        BrowseFacet facet = new BrowseFacet();
        facet.setHitCount(_counts[i]);
        facet.setValue(entry.value);
        facets.add(facet);
      }
      return facets;
    }

    public String getName()
    {
      return FilterMapFacetHandler.this.getName();
    }

	public List<BrowseFacet> combine(BrowseFacet facet, List<BrowseFacet> facets) {
		// TODO Auto-generated method stub
		return null;
	}

	public BrowseFacet getFacet(String value) {
		// TODO Auto-generated method stub
		return null;
	}
    
  }

  @Override
  public Object[] getRawFieldValues(int id) {
	return getFieldValues(id);
  }

}
