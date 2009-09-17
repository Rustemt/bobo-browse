package com.browseengine.bobo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.MappedFacetAccessible;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.impl.FacetHitcountComparatorFactory;

/**
 * @author ymatsuda
 *
 */
public class ListMerger
{
  public static class MergedIterator<T> implements Iterator<T>
  {
    private class IteratorNode
    {
      public Iterator<T> _iterator;
      public T _curVal;
      
      public IteratorNode(Iterator<T> iterator)
      {
        _iterator = iterator;
        _curVal = null;
      }
        
      public boolean fetch()
      {
        if(_iterator.hasNext())
        {
          _curVal = _iterator.next();
          return true;
        }
        _curVal = null;
        return false;
      }
    }

    private final PriorityQueue _queue;

    private MergedIterator(final int length, final Comparator<T> comparator)
    {
      _queue = new PriorityQueue()
      {
        {
          this.initialize(length);
        }
      
        @SuppressWarnings("unchecked")
        @Override
        protected boolean lessThan(Object o1, Object o2)
        {
          T v1 = ((IteratorNode)o1)._curVal;
          T v2 = ((IteratorNode)o2)._curVal;
          
          return (comparator.compare(v1, v2) < 0);
        }
      };
    }
    
    public MergedIterator(final List<Iterator<T>> iterators, final Comparator<T> comparator)
    {
      this(iterators.size(), comparator);
      for(Iterator<T> iterator : iterators)
      {
        IteratorNode ctx = new IteratorNode(iterator);
        if(ctx.fetch()) _queue.insert(ctx);
      }
    }
    
    public MergedIterator(final Iterator<T>[] iterators, final Comparator<T> comparator)
    {
      this(iterators.length, comparator);
      for(Iterator<T> iterator : iterators)
      {
        IteratorNode ctx = new IteratorNode(iterator);
        if(ctx.fetch()) _queue.insert(ctx);
      }
    }

    public boolean hasNext()
    {
      return _queue.size() > 0;
    }

    @SuppressWarnings("unchecked")
    public T next()
    {
      IteratorNode ctx = (IteratorNode)_queue.top();
      T val = ctx._curVal;
      if (ctx.fetch())
      {
        _queue.adjustTop();
      }
      else
      {
        _queue.pop();
      }
      return val;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  private ListMerger() { }
 
  public static <T> Iterator<T> mergeLists(final Iterator<T>[] iterators, final Comparator<T> comparator)
  {
    return new MergedIterator<T>(iterators, comparator);
  }
  
  public static <T> Iterator<T> mergeLists(final List<Iterator<T>> iterators, final Comparator<T> comparator)
  {
    return new MergedIterator<T>(iterators, comparator);
  }
  
  public static <T> ArrayList<T> mergeLists(int offset, int count, Iterator<T>[] iterators, Comparator<T> comparator)
  {
    return mergeLists(offset, count, new MergedIterator<T>(iterators, comparator));
  }
  
  public static <T> ArrayList<T> mergeLists(int offset, int count, List<Iterator<T>> iterators, Comparator<T> comparator)
  {
    return mergeLists(offset, count, new MergedIterator<T>(iterators, comparator));
  }
  
  private static <T> ArrayList<T> mergeLists(int offset, int count, Iterator<T> mergedIter)
  {
    for (int c = 0; c < offset && mergedIter.hasNext(); c++)
    {
      mergedIter.next();
    }
    
    ArrayList<T> mergedList = new ArrayList<T>();
    
    for (int c = 0; c < count && mergedIter.hasNext(); c++)
    {
      mergedList.add(mergedIter.next());
    }
    
    return mergedList;
  }
  
  public static Comparator<BrowseFacet> FACET_VAL_COMPARATOR = new Comparator<BrowseFacet>(){

	public int compare(BrowseFacet o1, BrowseFacet o2) {
		return o1.getValue().compareTo(o2.getValue());
	}
	  
  };
  
  
  public static Map<String,FacetAccessible> mergeSimpleFacetContainers(Collection<Map<String,FacetAccessible>> subMaps,BrowseRequest req)
  {
    Map<String, Map<String, Integer>> counts = new HashMap<String, Map<String, Integer>>();
    for (Map<String,FacetAccessible> subMap : subMaps)
    {
      for(Map.Entry<String, FacetAccessible> entry : subMap.entrySet())
      {
        Map<String, Integer> count = counts.get(entry.getKey());
        if(count == null)
        {
          count = new HashMap<String, Integer>();
          counts.put(entry.getKey(), count);
        }
        for(BrowseFacet facet : entry.getValue().getFacets())
        {
          String val = facet.getValue();
          int oldValue = count.containsKey(val) ? count.get(val) : 0;
          count.put(val, oldValue + facet.getHitCount());
        }
      }
    }

    Map<String, FacetAccessible> mergedFacetMap = new HashMap<String, FacetAccessible>();
    for(String facet : counts.keySet())
    {
      Map<String, Integer> facetValueCounts = counts.get(facet);
      FacetSpec fs = req.getFacetSpec(facet);
      
      FacetSpec.FacetSortSpec sortSpec = fs.getOrderBy();
      
      Comparator<BrowseFacet> comparator;
      if (FacetSortSpec.OrderValueAsc.equals(sortSpec)) 
    	  comparator = FACET_VAL_COMPARATOR;
      else if (FacetSortSpec.OrderHitsDesc.equals(sortSpec))
    	  comparator = FacetHitcountComparatorFactory.FACET_HITS_COMPARATOR;
      else comparator = fs.getCustomComparatorFactory().newComparator();
      
      List<BrowseFacet> facets = new ArrayList<BrowseFacet>(facetValueCounts.size());
      for(Entry<String, Integer> entry : facetValueCounts.entrySet())
      {
        facets.add(new BrowseFacet(entry.getKey(), entry.getValue()));
      }
      Collections.sort(facets, comparator);
      if (req != null)
      {
        FacetSpec fspec = req.getFacetSpec(facet);
        if (fspec!=null){
          int maxCount = fspec.getMaxCount();
          int numToShow = facets.size();
          if (maxCount>0){
        	  numToShow = Math.min(maxCount,numToShow);
          }
          facets = facets.subList(0, numToShow);
        }
      }
      MappedFacetAccessible mergedFacetAccessible = new MappedFacetAccessible(facets.toArray(new BrowseFacet[facets.size()]));
      mergedFacetMap.put(facet, mergedFacetAccessible);
    }
    return mergedFacetMap;
  }
}
