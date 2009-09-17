package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.util.BoundedPriorityQueue;

public class SimpleGroupbyFacetHandler extends FacetHandler implements FacetHandlerFactory{
	private final LinkedHashSet<String> _fieldsSet;
	private ArrayList<SimpleFacetHandler> _facetHandlers;
	private Map<String,SimpleFacetHandler> _facetHandlerMap;
	
	private static final String SEP=",";
	private int _maxdoc;
	private final String _sep;
	
	public SimpleGroupbyFacetHandler(String name, LinkedHashSet<String> dependsOn,String separator) {
		super(name, dependsOn);
		_fieldsSet = dependsOn;
		_facetHandlers = null;
		_facetHandlerMap = null;
		_maxdoc = 0;
		_sep = separator;
	}
	
	public SimpleGroupbyFacetHandler(String name, LinkedHashSet<String> dependsOn){
		this(name,dependsOn,SEP);
	}

	@Override
	public RandomAccessFilter buildRandomAccessFilter(String value,
			Properties selectionProperty) throws IOException {
		List<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>();
		String[] vals = value.split(_sep);
		for (int i = 0;i<vals.length;++i){
			SimpleFacetHandler handler = _facetHandlers.get(i);
			BrowseSelection sel = new BrowseSelection(handler.getName());
			sel.addValue(vals[i]);
			filterList.add(handler.buildFilter(sel));
		}
		return new RandomAccessAndFilter(filterList);
	}

	@Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel,
			FacetSpec fspec) {
		ArrayList<DefaultFacetCountCollector> collectorList = new ArrayList<DefaultFacetCountCollector>(_facetHandlers.size());
		for (SimpleFacetHandler facetHandler : _facetHandlers){
			collectorList.add((DefaultFacetCountCollector)facetHandler.getFacetCountCollector(sel, fspec));
		}
		return new GroupbyFacetCountCollector(_name,fspec, collectorList.toArray(new DefaultFacetCountCollector[collectorList.size()]),_maxdoc,_sep);
	}

	@Override
	public String[] getFieldValues(int id) {
		ArrayList<String> valList = new ArrayList<String>();
		for (FacetHandler handler : _facetHandlers){
			StringBuffer buf = new StringBuffer();
			boolean firsttime = true;
			String[] vals = handler.getFieldValues(id);
			if (vals!=null && vals.length > 0){
				if (!firsttime){
					buf.append(",");
				}
				else{
					firsttime=false;
				}
				for (String val : vals){
					buf.append(val);
				}
			}
			valList.add(buf.toString());
		}
		return valList.toArray(new String[valList.size()]);
	}

	@Override
	public ScoreDocComparator getScoreDocComparator() {
		ArrayList<ScoreDocComparator> comparatorList = new ArrayList<ScoreDocComparator>(_fieldsSet.size());
		for (FacetHandler handler : _facetHandlers){
			comparatorList.add(handler.getScoreDocComparator());
		}
		return new GroupbyScoreDocComparator(comparatorList.toArray(new ScoreDocComparator[comparatorList.size()]));
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
		_facetHandlers = new ArrayList<SimpleFacetHandler>(_fieldsSet.size());
		_facetHandlerMap = new HashMap<String,SimpleFacetHandler>(_fieldsSet.size());
		for (String name : _fieldsSet){
			FacetHandler handler = reader.getFacetHandler(name);
			if (handler==null || !(handler instanceof SimpleFacetHandler)){
				throw new IllegalStateException("only simple facet handlers supported");
			}
			SimpleFacetHandler sfh = (SimpleFacetHandler)handler;
			_facetHandlers.add(sfh);
			_facetHandlerMap.put(name, sfh);
		}
		_maxdoc = reader.maxDoc();
	}

	public FacetHandler newInstance() {
		return new SimpleGroupbyFacetHandler(_name,_fieldsSet);
	}
	
	
	private static class GroupbyScoreDocComparator implements ScoreDocComparator{
		private ScoreDocComparator[] _comparators;

		public GroupbyScoreDocComparator(ScoreDocComparator[] comparators) {
			_comparators = comparators;
		}
		
		public final int compare(ScoreDoc d1, ScoreDoc d2) {
			int retval=0;
			for (ScoreDocComparator comparator : _comparators){
				retval = comparator.compare(d1, d2);
				if (retval!=0) break;
			}
			return retval;
		}

		public final int sortType() {
			return SortField.CUSTOM;
		}

		public final Comparable sortValue(final ScoreDoc doc) {
			return new Comparable(){

				public int compareTo(Object o) {
					int retval = 0;
					for (ScoreDocComparator comparator : _comparators){
						retval = comparator.sortValue(doc).compareTo(o);
						if (retval != 0 ) break;
					}
					return retval;
				}
				
			};
		}
	}
	
	private static class GroupbyFacetCountCollector implements FacetCountCollector{
		private final DefaultFacetCountCollector[] _subcollectors;
		private final String _name;
		private final FacetSpec _fspec;
		private final int[] _count;
		private final int[] _lens;
		private final int _maxdoc;
		private final String _sep;
		
		public GroupbyFacetCountCollector(String name,FacetSpec fspec,DefaultFacetCountCollector[] subcollectors,int maxdoc,String sep){
			_name = name;
			_fspec = fspec;
			_subcollectors = subcollectors;
			_sep = sep;
			int totalLen=1;
			_lens = new int[_subcollectors.length];
			for (int i=0;i<_subcollectors.length;++i){
				_lens[i]=_subcollectors[i]._count.length;
				totalLen*=_lens[i];
			}
			_count = new int[totalLen];
			_maxdoc = maxdoc;
		}
		
		final public void collect(int docid) {
			int idx = 0;
			int i=0;
			int segsize=_count.length;
			for (DefaultFacetCountCollector subcollector : _subcollectors){
				segsize = segsize / _lens[i++];
				idx+=(subcollector._dataCache.orderArray.get(docid) * segsize);
			}
			_count[idx]++;
		}

		public void collectAll() {
			for (int i = 0; i < _maxdoc; ++i){
				collect(i);
			}
		}

		public int[] getCountDistribution() {
			return _count;
		}

		public String getName() {
			return _name;
		}

		public BrowseFacet getFacet(String value) {
			String[] vals = value.split(_sep);
			if (vals.length == 0) return null;
			StringBuffer buf = new StringBuffer();
			int startIdx=0;
			int segLen = _count.length;
			
			for (int i=0;i<vals.length;++i){
				if (i>0){
					buf.append(_sep);
				}
				int index=_subcollectors[i]._dataCache.valArray.indexOf(vals[i]);
				String facetName = _subcollectors[i]._dataCache.valArray.get(index);
				buf.append(facetName);
				
				segLen /= _subcollectors[i]._count.length;
				startIdx += index * segLen;
			}
			
			int count = _count[startIdx];
			for (int i = startIdx;i<startIdx+segLen;++i){
				count+=_count[i];
			}
			
			BrowseFacet f = new BrowseFacet(buf.toString(),count);
			return f;
		}
		
		private final String getFacetString(int idx){
			StringBuffer buf = new StringBuffer();
			int i=0;
			for (int len : _lens){
				if (i>0){
					buf.append(_sep);
				}
				
				int adjusted=idx*len;
				
				int bucket = adjusted/_count.length;
				buf.append(_subcollectors[i]._dataCache.valArray.get(bucket));
				idx=adjusted%_count.length;
				i++;
			}
			return buf.toString();
		}
		
		private final Object[] getRawFaceValue(int idx){
			Object[] retVal = new Object[_lens.length];
			int i=0;
			for (int len : _lens){
				int adjusted=idx*len;
				int bucket = adjusted/_count.length;
				retVal[i++]=_subcollectors[i]._dataCache.valArray.getInnerList().get(bucket);
				idx=adjusted%_count.length;
			}
			return retVal;
		}

		public List<BrowseFacet> getFacets() {
			if (_fspec!=null){
				int minCount=_fspec.getMinHitCount();
		          int max=_fspec.getMaxCount();
		          if (max <= 0) max=_count.length;

		          FacetSortSpec sortspec = _fspec.getOrderBy();
		          List<BrowseFacet> facetColl;
		          if (sortspec == FacetSortSpec.OrderValueAsc){
		        	  facetColl=new ArrayList<BrowseFacet>(max);
		              for (int i = 1; i < _count.length;++i) // exclude zero
		              {
		                int hits=_count[i];
		                if (hits>=minCount)
		                {
		                    BrowseFacet facet=new BrowseFacet(getFacetString(i),hits);
		                    facetColl.add(facet);
		                }
		                if (facetColl.size()>=max) break;
		              }
		          }
		          else{
		        	  ComparatorFactory comparatorFactory;
		        	  if (sortspec == FacetSortSpec.OrderHitsDesc){
		        		  comparatorFactory = new FacetHitcountComparatorFactory();
		        	  }
		        	  else{
		        		  comparatorFactory = _fspec.getCustomComparatorFactory();
		        	  }
		        	  
		        	  if (comparatorFactory == null){
		        		  throw new IllegalArgumentException("facet comparator factory not specified");
		        	  }
		        	  
		        	  Comparator<Integer> comparator = comparatorFactory.newComparator(new FieldValueAccessor(){

						public String getFormatedValue(int index) {
							return getFacetString(index);
						}

						public Object getRawValue(int index) {
							return getRawFaceValue(index);
						}
		        		  
		        	  }, _count);
		              facetColl=new LinkedList<BrowseFacet>();    
		              BoundedPriorityQueue<Integer> pq=new BoundedPriorityQueue<Integer>(comparator,max);
		              
		              for (int i=1;i<_count.length;++i) // exclude zero
		              {
		                int hits=_count[i];
		                if (hits>=minCount)
		                {
		                  if(!pq.offer(i))
		                  {
		                    // pq is full. we can safely ignore any facet with <=hits.
		                    minCount = hits + 1;
		                  }
		                }
		              }
		              
		              Integer val;
		              while((val = pq.poll()) != null)
		              {
		                  BrowseFacet facet=new BrowseFacet(getFacetString(val),_count[val]);
		                  ((LinkedList<BrowseFacet>)facetColl).addFirst(facet);
		              }
		          }
		          return facetColl;
			}
			else{
				return FacetCountCollector.EMPTY_FACET_LIST;
			}
		}
	}
}
