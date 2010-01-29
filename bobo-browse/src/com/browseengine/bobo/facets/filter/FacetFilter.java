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
		final public int docID() {
			return _doc;
		}

		@Override
		public int nextDoc() throws IOException {
		    while(_doc < _maxID) // not yet reached end
			{
				if (_orderArray.get(++_doc) == _index){
					return _doc;
				}
			}
			return DocIdSetIterator.NO_MORE_DOCS;
		}

		@Override
		public int advance(int id) throws IOException {
          if (_doc < id)
          {
            _doc = id - 1;
          }
		  
		  while(_doc < _maxID) // not yet reached end
		  {
		    if (_orderArray.get(++_doc) == _index){
		      return _doc;
		    }
		  }
		  return DocIdSetIterator.NO_MORE_DOCS;
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
        public DocIdSetIterator iterator() throws IOException
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
