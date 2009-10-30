package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.BoboIndexReader.WorkArea;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.MultiValueFacetFilter;
import com.browseengine.bobo.facets.filter.MultiValueORFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValueFacetHandler extends FacetHandler implements FacetHandlerFactory,FacetScoreable 
{
  private static Logger logger = Logger.getLogger(MultiValueFacetHandler.class);

  @Override
  public ScoreDocComparator getScoreDocComparator() 
  {
    return new MultiValueFacetDataCache.MultiFacetScoreDocComparator(_dataCache);
  }

  private final TermListFactory _termListFactory;
  private final String _indexFieldName;

  private int _maxItems = BigNestedIntArray.MAX_ITEMS;
  protected MultiValueFacetDataCache _dataCache;
  private Term _sizePayloadTerm;
  protected Set<String> _depends;
  
  public MultiValueFacetHandler(String name, 
                                String indexFieldName, 
                                TermListFactory termListFactory, 
                                Term sizePayloadTerm,
                                Set<String> depends) 
  {
    super(name, depends);
    _depends = depends;
    _indexFieldName = (indexFieldName != null ? indexFieldName : name);
    _termListFactory = termListFactory;
    _sizePayloadTerm = sizePayloadTerm;
    _dataCache = null;
  }
  
  public MultiValueFacetHandler(String name, String indexFieldName, TermListFactory termListFactory, Term sizePayloadTerm)
  {
    this(name, indexFieldName, termListFactory, sizePayloadTerm, null);
  }

  public MultiValueFacetHandler(String name, TermListFactory termListFactory, Term sizePayloadTerm) 
  {
    this(name, name, termListFactory, sizePayloadTerm, null);
  }

  public MultiValueFacetHandler(String name, String indexFieldName, TermListFactory termListFactory) 
  {
    this(name, indexFieldName, termListFactory, null, null);
  }

  public MultiValueFacetHandler(String name, TermListFactory termListFactory)
  {
    this(name, name, termListFactory);
  }

  public MultiValueFacetHandler(String name, String indexFieldName)
  {
    this(name, indexFieldName, null);
  }

  public MultiValueFacetHandler(String name)
  {
    this(name, name, null);
  }
  
  public MultiValueFacetHandler(String name, Set<String> depends)
  {
    this(name, name, null, null, depends);
  }

  public FacetHandler newInstance()
  {
    return new MultiValueFacetHandler(getName(), _indexFieldName, _termListFactory, _sizePayloadTerm, _depends);
  }

  public final MultiValueFacetDataCache getDataCache()
  {
    return _dataCache;
  }

  public void setMaxItems(int maxItems)
  {
    _maxItems = Math.min(maxItems,BigNestedIntArray.MAX_ITEMS);
  }

  @Override
  public String[] getFieldValues(int id) 
  {
    return _dataCache._nestedArray.getTranslatedData(id, _dataCache.valArray);
  }
  
  @Override
  public Object[] getRawFieldValues(int id){
    return new Object[]{_dataCache._nestedArray.getRawData(id, _dataCache.valArray)};
  }


  @Override
  public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec ospec)  
  {
    return new MultiValueFacetCountCollector(sel, _dataCache, _name, ospec);
  }

  @Override
  public void load(BoboIndexReader reader) throws IOException
  {
    load(reader, new WorkArea());
  }

  @Override
  public void load(BoboIndexReader reader, WorkArea workArea) throws IOException
  {
    if(_dataCache == null)
    {
      _dataCache = new MultiValueFacetDataCache();
    }

    _dataCache.setMaxItems(_maxItems);

    if(_sizePayloadTerm == null)
    {
      _dataCache.load(_indexFieldName, reader, _termListFactory, workArea);
    }
    else
    {
      _dataCache.load(_indexFieldName, reader, _termListFactory, _sizePayloadTerm);
    }
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    int index = _dataCache.valArray.indexOf(value);
    if(index >= 0) 
      return new MultiValueFacetFilter(_dataCache, index);
    else 
      return null;
  }

  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {

    ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);

    for (String val : vals)
    {
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
    RandomAccessFilter filter = null;
    
    int[] indexes = FacetDataCache.convert(_dataCache,vals);
    if (indexes.length > 1)
    {
      filter = new MultiValueORFacetFilter(_dataCache,indexes);
    }
    else if(indexes.length == 1)
    {
      filter = new MultiValueFacetFilter(_dataCache,indexes[0]);
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
  
  public BoboDocScorer getDocScorer(FacetTermScoringFunctionFactory scoringFunctionFactory,Map<String,Float> boostMap){
		float[] boostList = BoboDocScorer.buildBoostList(_dataCache.valArray, boostMap);
		return new MultiValueDocScorer(_dataCache,scoringFunctionFactory,boostList);
  }

  private static final class MultiValueDocScorer extends BoboDocScorer{
		private final MultiValueFacetDataCache _dataCache;
		private final BigNestedIntArray _array;
		
		MultiValueDocScorer(MultiValueFacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
			super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(), dataCache._nestedArray.size()),boostList);
			_dataCache = dataCache;
			_array = _dataCache._nestedArray;
		}
		
		@Override
		public Explanation explain(int doc){
			String[] vals = _array.getTranslatedData(doc, _dataCache.valArray);
			
			FloatList scoreList = new FloatArrayList(_dataCache.valArray.size());
			ArrayList<Explanation> explList = new ArrayList<Explanation>(scoreList.size());
			for (String val : vals)
			{
				int idx = _dataCache.valArray.indexOf(val);
				if (idx>=0){
				  scoreList.add(_function.score(_dataCache.freqs[idx], _boostList[idx]));
				  explList.add(_function.explain(_dataCache.freqs[idx], _boostList[idx]));
				}
			}
			Explanation topLevel = _function.explain(scoreList.toFloatArray());
			for (Explanation sub : explList){
				topLevel.addDetail(sub);
			}
			return topLevel;
		}
		
		@Override
		public final float score(int docid) {
			return _array.getScores(docid, _dataCache.freqs, _boostList, _function);
		}
		
	}

  private static final class MultiValueFacetCountCollector extends DefaultFacetCountCollector
  {
    private final BigNestedIntArray _array;
    MultiValueFacetCountCollector(BrowseSelection sel,
                                  FacetDataCache dataCache,
                                  String name,
                                  FacetSpec ospec)
                                  {
      super(sel,dataCache,name,ospec);
      _array = ((MultiValueFacetDataCache)(_dataCache))._nestedArray;
    }

    @Override
    public final void collect(int docid) 
    {
      _array.count(docid, _count);
    }

    @Override
    public final void collectAll()
    {
      _count = _dataCache.freqs;
    }
  }
}
