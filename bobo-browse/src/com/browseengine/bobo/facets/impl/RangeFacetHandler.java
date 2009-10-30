package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;

public class RangeFacetHandler extends FacetHandler implements FacetHandlerFactory
{
	private static Logger logger = Logger.getLogger(RangeFacetHandler.class);
	
	private FacetDataCache _dataCache;
	private final String _indexFieldName;
	private final TermListFactory _termListFactory;
	private final List<String> _predefinedRanges;
	private final boolean _autoRange;
	
	public RangeFacetHandler(String name,String indexFieldName,TermListFactory termListFactory,List<String> predefinedRanges)
	{
		super(name);
		_indexFieldName = indexFieldName;
		_dataCache = null;
		_termListFactory = termListFactory;
		_predefinedRanges = predefinedRanges;
		_autoRange = false;
	}
	
	public RangeFacetHandler(String name,TermListFactory termListFactory,List<String> predefinedRanges)
    {
	   this(name,name,termListFactory,predefinedRanges);
    }
	
	public RangeFacetHandler(String name,List<String> predefinedRanges)
    {
        this(name,name,null,predefinedRanges);
    }
	
	public RangeFacetHandler(String name,String indexFieldName,List<String> predefinedRanges)
    {
        this(name,indexFieldName,null,predefinedRanges);
    }
	
	public RangeFacetHandler(String name,String indexFieldName,TermListFactory termListFactory,boolean autoRange)
	{
		super(name);
		_dataCache = null;
		_indexFieldName = indexFieldName;
		_termListFactory = termListFactory;
		_predefinedRanges = null;
		_autoRange = autoRange;
	}
	
	public RangeFacetHandler(String name,TermListFactory termListFactory,boolean autoRange)
    {
        this(name,name,termListFactory,autoRange);
    }
	
	public RangeFacetHandler(String name,String indexFieldName,boolean autoRange)
    {
        this(name,indexFieldName,null,autoRange);
    }
	
	public RangeFacetHandler(String name,boolean autoRange)
	{
		this(name,name,null,autoRange);
	}
	
	public FacetHandler newInstance()
    {
	  if (_predefinedRanges == null)
	  {
        return new RangeFacetHandler(getName(),_indexFieldName,_termListFactory,_autoRange);
	  }
	  else
	  {
        return new RangeFacetHandler(getName(),_indexFieldName,_termListFactory,_predefinedRanges);
	  }
    }
	
	public boolean isAutoRange()
	{
		return _autoRange;
	}

	@Override
	public ScoreDocComparator getScoreDocComparator() {
		return _dataCache.getScoreDocComparator();
	}
	
	@Override
	public String[] getFieldValues(int id) {
		return new String[]{_dataCache.valArray.get(_dataCache.orderArray.get(id))};
	}
	
	@Override
	public Object[] getRawFieldValues(int id){
		return new Object[]{_dataCache.valArray.getRawValue(_dataCache.orderArray.get(id))};
	}

	private static String[] getRangeStrings(String rangeString)
	{
	  int index=rangeString.indexOf('[');
      int index2=rangeString.indexOf(" TO ");
      int index3=rangeString.indexOf(']');
      
      String lower,upper;
      
      lower=rangeString.substring(index+1,index2).trim();
      upper=rangeString.substring(index2+4,index3).trim();
      
      return new String[]{lower,upper};
	}
	
	static int[] parse(FacetDataCache dataCache,String rangeString)
	{
		String[] ranges = getRangeStrings(rangeString);
	    String lower=ranges[0];
	    String upper=ranges[1];
	    
	    if ("*".equals(lower))
	    {
	      lower=null;
	    }
	    
	    if ("*".equals(upper))
	    {
	      upper=null;
	    }
	    
	    int start,end;
	    if (lower==null)
	    {
	    	start=1;
	    }
	    else
	    {
	    	start=dataCache.valArray.indexOf(lower);
	    	if (start<0)
	    	{
	    		start=-(start + 1);
	    	}
	    }
	    
	    if (upper==null)
	    {
	    	end=dataCache.valArray.size()-1;
	    }
	    else
	    {
	    	end=dataCache.valArray.indexOf(upper);
	    	if (end<0)
	    	{
	    		end=-(end + 1);
	    		end=Math.max(0,end-1);
	    	}
	    }
	    
	    return new int[]{start,end};
	}
	
	public final FacetDataCache getDataCache()
	{
		return _dataCache;
	}
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    int[] range = parse(_dataCache,value);
    if(range != null) 
      return new FacetRangeFilter(_dataCache, range[0], range[1]);
    else
      return null;
  }
  
  public static int[] convertIndexes(FacetDataCache dataCache,String[] vals)
  {
    IntList list = new IntArrayList();
    for (String val : vals)
    {
      int[] range = parse(dataCache,val);
      if ( range!=null)
      {
        for (int i=range[0];i<=range[1];++i)
        {
          list.add(i);
        }
      }
    }
    return list.toIntArray();
  }
	
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {
    ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);
    
    for (String val : vals){
	  RandomAccessFilter f = buildRandomAccessFilter(val, prop);
	  if(f != null) 
	  {
	    filterList.add(f); 
	  }
      else
	  {
	    return EmptyFilter.getInstance();
	  }
    }
    
    if (filterList.size() == 1) return filterList.get(0);
    return new RandomAccessAndFilter(filterList);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    if (vals.length > 1)
    {
      return new FacetOrFilter(_dataCache,convertIndexes(_dataCache,vals),isNot);
    }
    else
    {
      RandomAccessFilter filter = buildRandomAccessFilter(vals[0],prop);
      if (filter == null) return filter;
      if (isNot)
      {
        filter = new RandomAccessNotFilter(filter);
      }
      return filter;
    }
  }

  @Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel,FacetSpec ospec) {
		return new RangeFacetCountCollector(_name,_dataCache,ospec,_predefinedRanges,_autoRange);
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
	    if (_dataCache == null)
	    {
	      _dataCache = new FacetDataCache();
	    }
		_dataCache.load(_indexFieldName, reader, _termListFactory);
	}
	
	@Override
	public FacetAccessible merge(FacetSpec fspec,
			List<FacetAccessible> facetList) {
		if (_autoRange) throw new IllegalStateException("Cannot support merging for autoRange");
		return super.merge(fspec, facetList);
	}

	
}
