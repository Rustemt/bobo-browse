package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.facets.filter.CompactMultiValueFacetFilter;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.StringArrayComparator;

public class CompactMultiValueFacetHandler extends FacetHandler implements FacetHandlerFactory,FacetScoreable
{
	private static Logger logger = Logger.getLogger(CompactMultiValueFacetHandler.class);
	
	private static final int MAX_VAL_COUNT = 32;
	private final TermListFactory _termListFactory;
	private FacetDataCache _dataCache;
	private final String _indexFieldName;

	public CompactMultiValueFacetHandler(String name,String indexFieldName,TermListFactory termListFactory) {
		super(name);
		_indexFieldName=indexFieldName;
		_termListFactory = termListFactory;
	}
	
	public CompactMultiValueFacetHandler(String name,TermListFactory termListFactory) {
      this(name,name,termListFactory);
    }
	
	public CompactMultiValueFacetHandler(String name,String indexFieldName) {
		this(name,indexFieldName,null);
	}
	
	public CompactMultiValueFacetHandler(String name) {
      this(name,name,null);
    }
	
	public FacetHandler newInstance()
    {
      return new CompactMultiValueFacetHandler(getName(),_indexFieldName,_termListFactory);
    }
	
	@Override
	public ScoreDocComparator getScoreDocComparator() {
		return new CompactMultiFacetScoreDocComparator(_dataCache);
	}
	
	public final FacetDataCache getDataCache()
	{
		return _dataCache;
	}
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value,Properties prop) throws IOException
  {
    int index = _dataCache.valArray.indexOf(value);
    if(index >= 0)
      return new CompactMultiValueFacetFilter(_dataCache, index);
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
    if(indexes.length > 0)
    {
      filter = new CompactMultiValueFacetFilter(_dataCache,indexes);
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
	public String[] getFieldValues(int id) {
		int encoded=_dataCache.orderArray.get(id);
		if (encoded==0) {
			return new String[]{""};
		}
		else{
			int count=1;
			ArrayList<String> valList=new ArrayList<String>(MAX_VAL_COUNT);
			
			while(encoded != 0)
			{
				if ((encoded & 0x00000001) != 0x0){
					valList.add(_dataCache.valArray.get(count));
				}
				count++;
				encoded >>>= 1;
			}
			return valList.toArray(new String[valList.size()]);
		}
	}

    @Override
	public Object[] getRawFieldValues(int id){
    	int encoded=_dataCache.orderArray.get(id);
		if (encoded==0) {
			return new Object[0];
		}
		else{
			int count=1;
			ArrayList<Object> valList=new ArrayList<Object>(MAX_VAL_COUNT);
			
			while(encoded != 0)
			{
				if ((encoded & 0x00000001) != 0x0){
					valList.add(_dataCache.valArray.getRawValue(count));
				}
				count++;
				encoded >>>= 1;
			}
			return valList.toArray(new Object[valList.size()]);
		}
	}
    
	@Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel,
			FacetSpec ospec) {
		return new CompactMultiValueFacetCountCollector(sel,_dataCache,_name,ospec);
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
		int maxDoc = reader.maxDoc();

		BigIntArray order = new BigIntArray(maxDoc);

		TermValueList mterms = _termListFactory == null ? new TermStringList() : _termListFactory.createTermList();
		
		IntArrayList minIDList=new IntArrayList();
	    IntArrayList maxIDList=new IntArrayList();
	    IntArrayList freqList = new IntArrayList();
	    
		TermDocs termDocs = null;
		TermEnum termEnum = null;
		int t = 0; // current term number
		mterms.add(null);
		minIDList.add(-1);
	    maxIDList.add(-1);
	    freqList.add(0);
		t++;
		try {
			termDocs = reader.termDocs();
			termEnum = reader.terms(new Term(_indexFieldName, ""));
			do {
				if (termEnum == null)
					break;
				Term term = termEnum.term();
				if (term == null || !_indexFieldName.equals(term.field()))
					break;

				// store term text
				// we expect that there is at most one term per document
				if (t > MAX_VAL_COUNT) {
					throw new IOException("maximum number of value cannot exceed: "
							+ MAX_VAL_COUNT);
				}
				String val = term.text();
				mterms.add(val);
				int bit = (0x00000001 << (t-1));
				termDocs.seek(termEnum);
				//freqList.add(termEnum.docFreq());  // removed because the df doesn't take into account the num of deletedDocs
				int df = 0;
				int minID=-1;
		        int maxID=-1;
		        if(termDocs.next())
		        {
		          df++;
                  int docid = termDocs.doc();
                  order.add(docid, order.get(docid) | bit);
                  minID = docid;
                  while (termDocs.next())
		          {
                    df++;
                    docid = termDocs.doc();
                    order.add(docid, order.get(docid) | bit);
		          }
				  maxID = docid;
		        }
	            freqList.add(df);
				minIDList.add(minID);
		        maxIDList.add(maxID);
				t++;
			} while (termEnum.next());
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} finally {
				if (termEnum != null) {
					termEnum.close();
				}
			}
		}
		
		mterms.seal();

		_dataCache=new FacetDataCache(order,mterms,freqList.toIntArray(),minIDList.toIntArray(),maxIDList.toIntArray(),TermCountSize.large);
	}
	
	private class CompactMultiFacetScoreDocComparator implements ScoreDocComparator{
        private final FacetDataCache _dataCache;
        private CompactMultiFacetScoreDocComparator(FacetDataCache dataCache){
          _dataCache = dataCache; 	
        }
        
		public int compare(ScoreDoc doc1, ScoreDoc doc2) {
			int encoded1=_dataCache.orderArray.get(doc1.doc);
			int encoded2=_dataCache.orderArray.get(doc2.doc);
			
			return encoded1-encoded2;
		}

		public int sortType() {
			return SortField.CUSTOM;
		}

		public Comparable sortValue(ScoreDoc sdoc) {
			return new StringArrayComparator(getFieldValues(sdoc.doc));
		}	
	}
	
	public BoboDocScorer getDocScorer(FacetTermScoringFunctionFactory scoringFunctionFactory,Map<String,Float> boostMap){
		float[] boostList = BoboDocScorer.buildBoostList(_dataCache.valArray, boostMap);
		return new CompactMultiValueDocScorer(_dataCache,scoringFunctionFactory,boostList);
	}
	
	private static final class CompactMultiValueDocScorer extends BoboDocScorer{
		private final FacetDataCache _dataCache;
		CompactMultiValueDocScorer(FacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
			super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(), dataCache.orderArray.size()),boostList);
			_dataCache = dataCache;
		}
		
		@Override
		public Explanation explain(int doc){
			int encoded=_dataCache.orderArray.get(doc);
			
			int count=1;
			FloatList scoreList = new FloatArrayList(_dataCache.valArray.size());
			ArrayList<Explanation> explList = new ArrayList<Explanation>(scoreList.size());
			while(encoded != 0)
			{
				if ((encoded & 0x00000001) != 0x0){
					int idx = count -1;
					scoreList.add(_function.score(_dataCache.freqs[idx], _boostList[idx]));
					explList.add(_function.explain(_dataCache.freqs[idx], _boostList[idx]));
				}
				count++;
				encoded >>>= 1;
			}
			Explanation topLevel = _function.explain(scoreList.toFloatArray());
			for (Explanation sub : explList){
				topLevel.addDetail(sub);
			}
			return topLevel;
		}
		
		@Override
		public final float score(int docid) {
			_function.clearScores();
			int encoded=_dataCache.orderArray.get(docid);
			
			int count=1;
			
			while(encoded != 0)
			{
				int idx = count -1;
				if ((encoded & 0x00000001) != 0x0){	
					_function.scoreAndCollect(_dataCache.freqs[idx], _boostList[idx]);
				}
				count++;
				encoded >>>= 1;
			}
			return _function.getCurrentScore();
		}
		
	}

	private static final class CompactMultiValueFacetCountCollector extends DefaultFacetCountCollector
	{
	  private final BigSegmentedArray _array;
	  private final int[] _combinationCount = new int[16 * 8];
	  private int _noValCount = 0;
	  private boolean _aggregated = false;
	  
	  
	  CompactMultiValueFacetCountCollector(BrowseSelection sel,
	                                       FacetDataCache dataCache,
	                                       String name,
	                                       FacetSpec ospec)
	                                       {
	    super(sel,dataCache,name,ospec);
	    _array = _dataCache.orderArray;
	  }
	  

	  @Override
	  public final void collectAll()
	  {
	    _count = _dataCache.freqs;
	    _aggregated = true;
	  }
	  
	  @Override
      public final void collect(int docid)
	  {
	    int encoded = _array.get(docid);
	    if(encoded == 0)
	    {
	      _noValCount++;
	    }
	    else
	    {
	      int offset = 0;
	      while(true)
	      {
	        _combinationCount[(encoded & 0x0F) + offset]++;
	        encoded = (encoded >>> 4);
	        if (encoded == 0) break;
	        offset += 16;
	      }
	    }
      }
	
	  @Override
	  public BrowseFacet getFacet(String value)
	  {
	    if(!_aggregated) aggregateCounts();
	    return super.getFacet(value);  
	  }
	  
      @Override
      public int[] getCountDistribution()
      {
        if(!_aggregated) aggregateCounts();
        return _count;
      }
      
      @Override
      public List<BrowseFacet> getFacets()
      {
        if(!_aggregated) aggregateCounts();
        return super.getFacets();
      }
      
      private void aggregateCounts()
      {
        _count[0] = _noValCount;
        
        for(int i = 1; i < _combinationCount.length; i++)
        {
          int count = _combinationCount[i];
          if(count > 0)
          {
            int offset = (i >> 4) * 4;
            int encoded = (i & 0x0F);
            int index = 1;
            while(encoded != 0)
            {
              if ((encoded & 0x00000001) != 0x0)
              {
                _count[index + offset] += count;
              }
              index++;
              encoded >>>= 1;
            }
          }
        }
        _aggregated = true;
      }
    }
}
