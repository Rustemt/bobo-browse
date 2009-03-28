package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.filter.FacetOrFilter.FacetOrDocIdSetIterator;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValueORFacetFilter extends RandomAccessFilter
{

  private static final long serialVersionUID = 1L;
  
  private final MultiValueFacetDataCache _dataCache;
  private final OpenBitSet _bitset;
  protected final int[] _index;
  
  public MultiValueORFacetFilter(MultiValueFacetDataCache dataCache,int[] index)
  {
    _dataCache = dataCache;
    _index = index;
    _bitset = new OpenBitSet(_dataCache.valArray.size());
    for (int i : _index)
    {
      _bitset.fastSet(i);
    }  
  }
  
  private final static class MultiValueFacetDocIdSetIterator extends FacetOrDocIdSetIterator
  {
      private BigNestedIntArray _nestedArray;
      public MultiValueFacetDocIdSetIterator(MultiValueFacetDataCache dataCache, int[] index,OpenBitSet bs) 
      {
        super(dataCache,index,bs);
        _nestedArray = dataCache._nestedArray;
      }
      
      @Override
      final public boolean next() throws IOException {
          while(_doc < _maxID) // not yet reached end
          {
              _doc++;
              if (_nestedArray.contains(_doc, _bitset)){
                  return true;
              }
          }
          return false;
      }

      @Override
      final public boolean skipTo(int id) throws IOException {
        if (_doc < id)
        {
          _doc=id-1;
        }
        
        while(_doc < _maxID) // not yet reached end
        {
          _doc++;
          if (_nestedArray.contains(_doc, _bitset)){
            return true;
          }
        }
        return false;
      }
  }
  
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(IndexReader reader) throws IOException
  {
    if (_index.length == 0)
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
                return new MultiValueFacetDocIdSetIterator(_dataCache,_index,_bitset);
            }

    @Override
    final public boolean get(int docId)
    {
      return _dataCache._nestedArray.contains(docId,_bitset);
    }
        };
    }
  }

}
