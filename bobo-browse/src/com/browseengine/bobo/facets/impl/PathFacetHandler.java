package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.MultiValueORFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;

public class PathFacetHandler extends FacetHandler implements FacetHandlerFactory 
{
	private static final String DEFAULT_SEP = "/";
	
	public static final String SEL_PROP_NAME_STRICT="strict";
    public static final String SEL_PROP_NAME_DEPTH="depth";
    
    private final boolean _multiValue;
	
	private FacetDataCache _dataCache;
	private final TermListFactory _termListFactory;
	private String _separator;
	
	public PathFacetHandler(String name){
		this(name,false);
	}
	
	public PathFacetHandler(String name,boolean multiValue)
	{
		super(name);
		_multiValue = multiValue;
		_dataCache=null;
		_termListFactory=TermListFactory.StringListFactory;
		_separator=DEFAULT_SEP;
	}
	
	public FacetHandler newInstance()
	{
		return new PathFacetHandler(getName(),_multiValue);
	}
	
	public final FacetDataCache getDataCache()
	{
		return _dataCache;
	}
	
	/**
     * Sets is strict applied for counting. Used if the field is of type <b><i>path</i></b>.
     * @param strict is strict applied
     */
    public static void setStrict(Properties props,boolean strict) {
      props.setProperty(PathFacetHandler.SEL_PROP_NAME_STRICT, String.valueOf(strict));
    }
    

    /**
     * Sets the depth.  Used if the field is of type <b><i>path</i></b>.
     * @param depth depth
     */
    public static void setDepth(Properties props,int depth) {
      props.setProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, String.valueOf(depth));
    }
    

	/**
     * Gets if strict applied for counting. Used if the field is of type <b><i>path</i></b>.
     * @return is strict applied
     */
    public static boolean isStrict(Properties selectionProp) {
    	try
    	{
          return Boolean.valueOf(selectionProp.getProperty(PathFacetHandler.SEL_PROP_NAME_STRICT));
    	}
    	catch(Exception e)
    	{
    		return false;
    	}
    }
    
    /**
     * Gets the depth.  Used if the field is of type <b><i>path</i></b>.
     * @return depth
     */
    public static int getDepth(Properties selectionProp) {
      try
      {
        return Integer.parseInt(selectionProp.getProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH));
      }
      catch(Exception e)
      {
        return 1;
      }
    }
    
	@Override
	public ScoreDocComparator getScoreDocComparator()  
	{
		return _dataCache.getScoreDocComparator();
	}
	@Override
	public String[] getFieldValues(int id) 
	{
		if (_multiValue){
		  return ((MultiValueFacetDataCache)_dataCache)._nestedArray.getTranslatedData(id, _dataCache.valArray);	
		}
		else{
		  return new String[]{_dataCache.valArray.get(_dataCache.orderArray.get(id))};
		}
	}
	 
	@Override
	public Object[] getRawFieldValues(int id){
		return getFieldValues(id);
	}

	
	public void setSeparator(String separator)
	{
		_separator = separator;
	}
	
	public String getSeparator()
	{
		return _separator;
	}
	
	private int getPathDepth(String path)
	{
		return path.split(String.valueOf(_separator)).length;
	}
	
	private void getFilters(IntSet intSet,String[] vals, int depth, boolean strict)
    {
	 for (String val : vals)
	 {
	   getFilters(intSet,val,depth,strict);
	 }
    }
	
	private void getFilters(IntSet intSet,String val, int depth, boolean strict)
	{
	    List<String> termList = _dataCache.valArray;
		int index = termList.indexOf(val);

		int startDepth = getPathDepth(val);
		
		if (index < 0)
		{
			int nextIndex = -(index + 1);
			if (nextIndex == termList.size())
			{
				return;
			}	
			index = nextIndex;
		}
		

		for (int i=index; i<termList.size(); ++i)
		{
			String path = termList.get(i);
			if (path.startsWith(val))
			{
				if (!strict || getPathDepth(path) - startDepth == depth)
				{
				  intSet.add(i);
				}
			}
			else
			{
				break;
			}	
		}
	}
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value,Properties props) throws IOException
  {
    int depth = getDepth(props);
    boolean strict = isStrict(props);
    IntSet intSet = new IntOpenHashSet();
    getFilters(intSet,value, depth, strict);
    if (intSet.size()>0)
    {
      int[] indexes = intSet.toIntArray();
      return _multiValue ? new MultiValueORFacetFilter((MultiValueFacetDataCache)_dataCache, indexes) : new FacetOrFilter(_dataCache,indexes);
    }
    else
    {
      return null;
    }
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {
    if (vals.length > 1)
    {
      return EmptyFilter.getInstance();
    }
    else
    {
      RandomAccessFilter f = buildRandomAccessFilter(vals[0], prop);
      if (f!=null)
      {
        return f;
      }
      else
      {
        return EmptyFilter.getInstance();
      }
    }
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    if (vals.length > 1)
    {
      int depth = getDepth(prop);
      boolean strict = isStrict(prop);
      IntSet intSet = new IntOpenHashSet();
      getFilters(intSet,vals,depth,strict);
      if (intSet.size()>0)
      {
    	return _multiValue ? new MultiValueORFacetFilter((MultiValueFacetDataCache)_dataCache, intSet.toIntArray(),isNot) : new FacetOrFilter(_dataCache,intSet.toIntArray(),isNot);
      }
      else
      {
        if (isNot)
        {
          return null;
        }
        else
        {
          return EmptyFilter.getInstance();
        }
      }
    }
    else
    {
      RandomAccessFilter f = buildRandomAccessFilter(vals[0], prop);
      if (f == null) return f;
      if (isNot)
      {
        f = new RandomAccessNotFilter(f);
      }
      return f;
    }
  }
	
	@Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec ospec) 
	{
		return _multiValue ? new MultiValuedPathFacetCountCollector(_name, _separator, sel, ospec, _dataCache) : 
							 new PathFacetCountCollector(_name,_separator,sel,ospec,_dataCache);
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
	    if (_dataCache == null)
	    {
	      if (!_multiValue){
	        _dataCache = new FacetDataCache();
	      }
	      else{
	    	  _dataCache = new MultiValueFacetDataCache();
	      }
	    }
		_dataCache.load(_name, reader, _termListFactory);
	}
	
	public static void main(String[] args) throws Exception{
		/*String start = args[0];
		int depth=Integer.parseInt(args[1]);
		int count = Integer.parseInt(args[2]);
		*/
		String field ="makemodel";
		String start="asian";
		int depth = 100;
		int count = 2;
		
		File idxDir = new File("/Users/john/opensource/bobo-trunk/cardata/cartag");
		IndexReader reader = IndexReader.open(idxDir);
		BoboIndexReader breader = BoboIndexReader.getInstance(reader);
		
		BoboBrowser boboBrowse = new BoboBrowser(breader);
		BrowseRequest req = new BrowseRequest();
		BrowseSelection sel = new BrowseSelection(field);
		if (start!=null){
		  sel.addValue(start);
		}
		sel.setSelectionProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, String.valueOf(depth));
		req.addSelection(sel);
		FacetSpec fspec = new FacetSpec();
		fspec.setMaxCount(count);
		req.setFacetSpec(field, fspec);
		
		BrowseResult res = boboBrowse.browse(req);
		System.out.println(res);
	}

}
