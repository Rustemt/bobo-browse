package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BoundedPriorityQueue;

public abstract class DefaultFacetCountCollector implements FacetCountCollector
{
  protected final FacetSpec _ospec;
  protected int[] _count;
  protected final FacetDataCache _dataCache;
  private final String _name;
  protected final BrowseSelection _sel;
  protected final BigIntArray _array;
  
  public static class FacetHitsComparator implements Comparator<BrowseFacet>{

	public int compare(BrowseFacet o1, BrowseFacet o2) {
		// TODO Auto-generated method stub
		return 0;
	}
	  
  }
  
  public DefaultFacetCountCollector(BrowseSelection sel,FacetDataCache dataCache,String name,FacetSpec ospec)
  {
      _sel = sel;
      _ospec = ospec;
      _name = name;
      _dataCache = dataCache;
      _count = new int[_dataCache.freqs.length];
      _array = _dataCache.orderArray;
  }
  
  public String getName()
  {
      return _name;
  }
  
  abstract public void collect(int docid);
  
  abstract public void collectAll();
  
  public BrowseFacet getFacet(String value)
  {
      BrowseFacet facet = null;
      int index=_dataCache.valArray.indexOf(value);
      if (index >=0 ){
          facet = new BrowseFacet(_dataCache.valArray.get(index),_count[index]);
      }
      else{
    	facet = new BrowseFacet(_dataCache.valArray.format(value),0);  
      }
      return facet; 
  }
  
  public int[] getCountDistribution()
  {
    return _count;
  }

  public List<BrowseFacet> getFacets() {
      if (_ospec!=null)
      {
          int minCount=_ospec.getMinHitCount();
          int max=_ospec.getMaxCount();
          if (max <= 0) max=_count.length;

          List<BrowseFacet> facetColl;
          List<String> valList=_dataCache.valArray;
          FacetSortSpec sortspec = _ospec.getOrderBy();
          if (sortspec == FacetSortSpec.OrderValueAsc)
          {
              facetColl=new ArrayList<BrowseFacet>(max);
              for (int i = 1; i < _count.length;++i) // exclude zero
              {
                int hits=_count[i];
                if (hits>=minCount)
                {
                    BrowseFacet facet=new BrowseFacet(valList.get(i),hits);
                    facetColl.add(facet);
                }
                if (facetColl.size()>=max) break;
              }
          }
          else //if (sortspec == FacetSortSpec.OrderHitsDesc)
          {
              facetColl=new LinkedList<BrowseFacet>();    
              BoundedPriorityQueue<Integer> pq=new BoundedPriorityQueue<Integer>(new Comparator<Integer>(){

                  public int compare(Integer f1, Integer f2) {
                      int val=_count[f1] - _count[f2];
                      if (val==0)
                      {
                          val=-(f1-f2);
                      }
                      return val;
                  }
                  
              },max);
              
              int size = valList.size();
              for (int i=1;i<size;++i) // exclude zero
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
                  BrowseFacet facet=new BrowseFacet(valList.get(val),_count[val]);
                  ((LinkedList<BrowseFacet>)facetColl).addFirst(facet);
              }
          }
          return facetColl;
      }
      else
      {
          return FacetCountCollector.EMPTY_FACET_LIST;
      }
  }
}
