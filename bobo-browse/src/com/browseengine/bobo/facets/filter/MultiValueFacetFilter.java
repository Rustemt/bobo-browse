package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.filter.FacetFilter.FacetDocIdSetIterator;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValueFacetFilter extends RandomAccessFilter 
{
    private static final long serialVersionUID = 1L;
    
    private final MultiValueFacetDataCache _dataCache;
    private final BigNestedIntArray _nestedArray;
    private final int _index;
    
    public MultiValueFacetFilter(MultiValueFacetDataCache dataCache,int index)
    {
        _dataCache = dataCache;
        _nestedArray = dataCache._nestedArray;
        _index = index;
    }
    
    private final static class MultiValueFacetDocIdSetIterator extends FacetDocIdSetIterator
    {
        private final BigNestedIntArray _nestedArray;

        public MultiValueFacetDocIdSetIterator(MultiValueFacetDataCache dataCache, int index) 
        {
            super(dataCache, index);
            _nestedArray = dataCache._nestedArray;
        }
        
        @Override
        final public int nextDoc() throws IOException {
            while(_doc < _maxID) // not yet reached end
            {
                if (_nestedArray.contains(++_doc, _index)){
                    return _doc;
                }
            }
            return DocIdSetIterator.NO_MORE_DOCS;
        }

        @Override
        final public int advance(int id) throws IOException {
          
          if(id > _doc)
          {
            _doc = id - 1;
            return nextDoc();
          }
          
          return nextDoc();
        }
    }

    @Override
    public RandomAccessDocIdSet getRandomAccessDocIdSet(IndexReader reader) throws IOException {
        if(_index < 0)
        {
            return EmptyDocIdSet.getInstance();
        }
        else
        {
            return new RandomAccessDocIdSet()
            {
                @Override
                public DocIdSetIterator iterator() 
                {
                    return new MultiValueFacetDocIdSetIterator(_dataCache, _index);
                }
        @Override
        final public boolean get(int docId)
        {
          return _nestedArray.contains(docId, _index);
        }
                
            };
        }
    }

}
