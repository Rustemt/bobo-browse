package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;

public class FacetFilter extends RandomAccessFilter 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected final FacetDataCache _dataCache;
    protected final BigSegmentedArray _orderArray;
	protected final int _index;
	
	public FacetFilter(FacetDataCache dataCache, int index)
	{
		_dataCache = dataCache;
		_orderArray = dataCache.orderArray;
		_index=index;
	}
	
	public static class FacetDocIdSetIterator extends DocIdSetIterator
	{
		protected int _doc;
		protected final int _index;
		protected final int _maxID;
        protected final BigSegmentedArray _orderArray;
		
		public FacetDocIdSetIterator(FacetDataCache dataCache,int index)
		{
			_index=index;
			_doc=Math.max(-1,dataCache.minIDs[_index]-1);
			_maxID = dataCache.maxIDs[_index];
			_orderArray = dataCache.orderArray;
		}
		
		@Override
		final public int doc() {
			return _doc;
		}
		/*
		protected boolean validate(int docid){
			return _dataCache.orderArray.get(docid) == _index;
		}
*/
		@Override
		public boolean next() throws IOException {
          _doc = _orderArray.findValue(_index, _doc + 1, _maxID);
          return (_doc <= _maxID);
		}

		@Override
		public boolean skipTo(int id) throws IOException {
          if(id < _doc) id = _doc + 1;
          _doc = _orderArray.findValue(_index, id, _maxID);
          return (_doc <= _maxID);
		}

	}

	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(IndexReader reader) throws IOException 
	{
		if (_index < 0)
		{
		  final DocIdSet empty = EmptyDocIdSet.getInstance();
			return new RandomAccessDocIdSet()
			{
        @Override
        public boolean get(int docId)
        {
          return false;
        }

        @Override
        public DocIdSetIterator iterator()
        {
          return empty.iterator();
        }		  
			};
		}
		else
		{
			return new RandomAccessDocIdSet()
			{
				@Override
				public DocIdSetIterator iterator() 
				{
					return new FacetDocIdSetIterator(_dataCache,_index);
				}

        @Override
        final public boolean get(int docId)
        {
          return _orderArray.get(docId) == _index;
        }
			};
		}
	}

}
