package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;

public class FacetOrFilter extends RandomAccessFilter
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  protected final FacetDataCache _dataCache;
  protected final BigSegmentedArray _orderArray;
  protected final int[] _index;
  private OpenBitSet _bitset;
  
  public FacetOrFilter(FacetDataCache dataCache, int[] index)
  {
    this(dataCache,index,false);
  }
  
  public FacetOrFilter(FacetDataCache dataCache, int[] index,boolean takeCompliment)
  {
    _dataCache = dataCache;
    _orderArray = dataCache.orderArray;
    _index = index;
    _bitset = new OpenBitSet(_dataCache.valArray.size());
    for (int i : _index)
    {
      _bitset.fastSet(i);
    }
    if (takeCompliment)
    {
      _bitset.flip(0, _dataCache.valArray.size());
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
                return new FacetOrDocIdSetIterator(_dataCache,_index,_bitset);
            }

    @Override
    final public boolean get(int docId)
    {
      return _bitset.fastGet(_orderArray.get(docId));
    }
        };
    }
  }
  
  public static class FacetOrDocIdSetIterator extends DocIdSetIterator
  {
      protected int _doc;
      protected final FacetDataCache _dataCache;
      protected final int[] _index;
      protected int _maxID;
      protected final OpenBitSet _bitset;
      protected final BigSegmentedArray _orderArray;
      
      public FacetOrDocIdSetIterator(FacetDataCache dataCache,int[] index,OpenBitSet bitset)
      {
          _dataCache=dataCache;
          _index=index;
          _orderArray = dataCache.orderArray;
          _bitset=bitset;
              
          _doc = Integer.MAX_VALUE;
          _maxID = -1;
          for (int i : _index)
          {
            if (_doc > _dataCache.minIDs[i]){
              _doc = _dataCache.minIDs[i];
            }
            if (_maxID < _dataCache.maxIDs[i])
            {
              _maxID = _dataCache.maxIDs[i];
            }
          }
          _doc--;
          if (_doc<0) _doc=-1;
      }
      
      @Override
      final public int docID() {
          return _doc;
      }
      /*
      protected boolean validate(int docid){
          return _dataCache.orderArray.get(docid) == _index;
      }
*/
      @Override
      public int nextDoc() throws IOException {
          while(_doc < _maxID) // not yet reached end
          {
              if (_bitset.fastGet(_orderArray.get(++_doc))){
                  return _doc;
              }
          }
          return DocIdSetIterator.NO_MORE_DOCS;
      }

      @Override
      public int advance(int id) throws IOException {
        if (_doc < id)
        {
          _doc=id-1;
        }
        
        while(_doc < _maxID) // not yet reached end
        {
          if (_bitset.fastGet(_orderArray.get(++_doc))){
            return _doc;
          }
        }
        return DocIdSetIterator.NO_MORE_DOCS;
      }

  }

}
