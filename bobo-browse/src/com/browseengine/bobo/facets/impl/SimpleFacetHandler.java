package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunction;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;

public class SimpleFacetHandler extends FacetHandler implements FacetHandlerFactory,FacetScoreable
{
	private static Logger logger = Logger.getLogger(SimpleFacetHandler.class);
	private FacetDataCache _dataCache;
	private final TermListFactory _termListFactory;
	private final String _indexFieldName;
	
	public SimpleFacetHandler(String name,String indexFieldName,TermListFactory termListFactory)
	{
		super(name);
		_indexFieldName=indexFieldName;
		_dataCache=null;
		_termListFactory=termListFactory;
	}
	
	public SimpleFacetHandler(String name,TermListFactory termListFactory)
    {
        this(name,name,termListFactory);
    }
	
	public SimpleFacetHandler(String name)
    {
        this(name,name,null);
    }
	
	public SimpleFacetHandler(String name,String indexFieldName)
	{
		this(name,indexFieldName,null);
	}
	
	public FacetHandler newInstance()
	{
	  return new SimpleFacetHandler(getName(),_indexFieldName,_termListFactory);
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
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    int index = _dataCache.valArray.indexOf(value);
    if (index >= 0) 
      return new FacetFilter(_dataCache, index);
    else 
      return null;
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
      return buildRandomAccessFilter(vals[0],prop);
    }
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    RandomAccessFilter filter = null;
    
    int[] indexes = FacetDataCache.convert(_dataCache,vals);
    if(indexes.length > 1)
    {
      return new FacetOrFilter(_dataCache,indexes,isNot);
    }
    else if(indexes.length == 1)
    {
      filter = new FacetFilter(_dataCache, indexes[0]);
    }
    else
    {
      filter = EmptyFilter.getInstance();
    }
    if (isNot)
    {
      filter = new RandomAccessNotFilter(filter);
    }
    return filter;
  }

  @Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel,FacetSpec ospec) {
		return new SimpleFacetCountCollector(sel,_dataCache,_name,ospec);
	}
	
	public final FacetDataCache getDataCache()
	{
		return _dataCache;
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
	    if (_dataCache==null) 
	    {
	      _dataCache=new FacetDataCache();
	    }
		_dataCache.load(_indexFieldName, reader, _termListFactory);
	}
	
	public BoboDocScorer getDocScorer(FacetTermScoringFunctionFactory scoringFunctionFactory,Map<String,Float> boostMap){
		float[] boostList = BoboDocScorer.buildBoostList(_dataCache.valArray, boostMap);
		return new SimpleBoboDocScorer(_dataCache,scoringFunctionFactory,boostList);
	}
	
	public static final class SimpleFacetCountCollector extends DefaultFacetCountCollector
	{
		public SimpleFacetCountCollector(BrowseSelection sel,FacetDataCache dataCache,String name,FacetSpec ospec)
		{
		    super(sel,dataCache,name,ospec);
		}
		
		public final void collect(int docid) {
			_count[_array.get(docid)]++;
		}
		
		public final void collectAll() {
		  _count = _dataCache.freqs;
        }
	}
	
	public static final class SimpleBoboDocScorer extends BoboDocScorer{
		private final FacetDataCache _dataCache;
		
		public SimpleBoboDocScorer(FacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
			super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(), dataCache.orderArray.size()),boostList);
			_dataCache = dataCache;
		}
		
		@Override
		public Explanation explain(int doc){
			int idx = _dataCache.orderArray.get(doc);
			return _function.explain(_dataCache.freqs[idx],_boostList[idx]);
		}

		@Override
		public final float score(int docid) {
			int idx = _dataCache.orderArray.get(docid);
			return _function.score(_dataCache.freqs[idx],_boostList[idx]);
		}
	}
}
