package com.browseengine.bobo.facets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.RandomAccessOrFilter;
import com.browseengine.bobo.util.BoundedPriorityQueue;

/**
 * FacetHandler definition
 *
 */
public abstract class FacetHandler implements Cloneable 
{
	protected final String _name;
	private final Set<String> _dependsOn;
	private final Map<String,FacetHandler> _dependedFacetHandlers;
	private TermCountSize _termCountSize;
	
	public static enum TermCountSize{
		small,
		medium,
		large
	}
	
	/**
	 * Constructor
	 * @param name name
	 * @param dependsOn Set of names of facet handlers this facet handler depend on for loading
	 */
	public FacetHandler(String name,Set<String> dependsOn)
	{
		_name=name;
		_dependsOn = new HashSet<String>();
		if (dependsOn != null)
		{
			_dependsOn.addAll(dependsOn);
		}
		_dependedFacetHandlers = new HashMap<String,FacetHandler>();
		_termCountSize = TermCountSize.large;
	}
	
	public void setTermCountSize(String termCountSize){
		setTermCountSize(TermCountSize.valueOf(termCountSize.toLowerCase()));
	}
	
	public void setTermCountSize(TermCountSize termCountSize){
		_termCountSize = termCountSize;
	}
	
	public TermCountSize getTermCountSize(){
		return _termCountSize;
	}
	
	/**
	 * Constructor
	 * @param name name
	 */
	public FacetHandler(String name)
	{
		this(name,null);
	}
	
	/**
	 * Gets the name
	 * @return name
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * Gets names of the facet handler this depends on
	 * @return set of facet handler names
	 */
	public final Set<String> getDependsOn()
	{
		return _dependsOn;
	}
	
	/**
	 * Adds a list of depended facet handlers
	 * @param facetHandler depended facet handler
	 */
	public final void putDependedFacetHandler(FacetHandler facetHandler)
	{
		_dependedFacetHandlers.put(facetHandler._name, facetHandler);
	}
	
	/**
	 * Gets a depended facet handler
	 * @param name facet handler name
	 * @return facet handler instance 
	 */
	public final FacetHandler getDependedFacetHandler(String name)
	{
		return _dependedFacetHandlers.get(name);
	}
	
	/**
	 * Load information from an index reader, initialized by {@link BoboIndexReader}
	 * @param reader reader
	 * @throws IOException
	 */
	abstract public void load(BoboIndexReader reader) throws IOException;
	
	private static class CombinedFacetAccessible implements FacetAccessible
	{
		private final List<FacetAccessible> _list;
		private final FacetSpec _fspec;
		CombinedFacetAccessible(FacetSpec fspec,List<FacetAccessible> list)
		{
			_list = list;
			_fspec = fspec;
		}
		
		public String toString() {
			return "_list:"+_list+" _fspec:"+_fspec;
		}
		
		public BrowseFacet getFacet(String value) {
			int sum=-1;
			String foundValue=null;
			if (_list!=null)
			{
				for (FacetAccessible facetAccessor : _list)
				{
					BrowseFacet facet = facetAccessor.getFacet(value);
					if (facet!=null)
					{
					  foundValue = facet.getValue();
						if (sum==-1) sum=facet.getHitCount();
						else sum+=facet.getHitCount();
					}
				}
			}
			if (sum==-1) return null;
			return new BrowseFacet(foundValue,sum);
		}

		public List<BrowseFacet> getFacets() {
			Map<String,BrowseFacet> facetMap;
			if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
			{
				facetMap= new TreeMap<String,BrowseFacet>();
			}
			else
			{
				facetMap = new HashMap<String,BrowseFacet>();
			}
			
			for (FacetAccessible facetAccessor : _list)
			{
				Iterator<BrowseFacet> iter = facetAccessor.getFacets().iterator();
				if (facetMap.size() == 0)
				{
					while(iter.hasNext())
					{
						BrowseFacet facet = iter.next();
						facetMap.put(facet.getValue(),facet);
					}
				}
				else
				{
					while(iter.hasNext())
					{
						BrowseFacet facet = iter.next();
						BrowseFacet existing = facetMap.get(facet.getValue());
						if (existing == null)
						{
							facetMap.put(facet.getValue(), facet);
						}
						else
						{
							existing.setHitCount(existing.getHitCount() + facet.getHitCount());
						}
					}
				}
			}
			
            int cnt = 0;
            int maxCnt = _fspec.getMaxCount();
            if(maxCnt <= 0) maxCnt = Integer.MAX_VALUE;
            int minHits = _fspec.getMinHitCount();
            List<BrowseFacet> list = new LinkedList<BrowseFacet>();
			
			if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
			{
			  for(BrowseFacet facet : facetMap.values())
			  {
			    if(facet.getHitCount() >= minHits)
			    {
			      list.add(facet);
			      if(++cnt >= maxCnt) break;			      
			    }
			  }
			}
			else
			{
			  Comparator<BrowseFacet> comparator;
			  if (FacetSortSpec.OrderHitsDesc.equals(_fspec.getOrderBy()))
			  {
			    comparator = new Comparator<BrowseFacet>()
			    {
			      public int compare(BrowseFacet f1, BrowseFacet f2)
			      {
			        int val=f2.getHitCount() - f1.getHitCount();
			        if (val==0)
			        {
			          val = (f1.getValue().compareTo(f2.getValue()));
			        }
			        return val;
			      }
                };
			  }
			  else // FacetSortSpec.OrderByCustom.equals(_fspec.getOrderBy()
			  {
			    comparator = _fspec.getCustomComparatorFactory().newComparator();
			  }
			  ArrayList<BrowseFacet> facets = new ArrayList<BrowseFacet>(facetMap.values());
			  Collections.sort(facets, comparator);
			  for(BrowseFacet facet : facets)
			  {
			    if(facet.getHitCount() >= minHits)
			    {
			      list.add(facet);
			      if(++cnt >= maxCnt) break;                  
			    }
			  }
			}
			return list;
		}
		
	}
	
	public FacetAccessible merge(FacetSpec fspec, List<FacetAccessible> facetList)
	{
		return new CombinedFacetAccessible(fspec,facetList);
	}
	
	public void load(BoboIndexReader reader, BoboIndexReader.WorkArea workArea) throws IOException
	{
	  load(reader);
	}
	
	/**
	 * Gets a filter from a given selection
	 * @param sel selection
	 * @return a filter
	 * @throws IOException 
	 * @throws IOException
	 */
	public final RandomAccessFilter buildFilter(BrowseSelection sel) throws IOException
	{
      String[] selections = sel.getValues();
      String[] notSelections = sel.getNotValues();
      Properties prop=sel.getSelectionProperties();
      
      RandomAccessFilter filter = null;
      if (selections!=null && selections.length > 0)
      {
        if (sel.getSelectionOperation() == ValueOperation.ValueOperationAnd)
        {
          filter = buildRandomAccessAndFilter(selections,prop);
          if (filter == null)
          {
            filter = EmptyFilter.getInstance();
          }
        }
        else
        {
          filter = buildRandomAccessOrFilter(selections, prop,false);
          if (filter == null)
          {
            return EmptyFilter.getInstance();
          }
        }
      }
      
      if (notSelections!=null && notSelections.length>0)
      {
        RandomAccessFilter notFilter = buildRandomAccessOrFilter(notSelections, prop, true);
        if (filter==null)
        {
          filter = notFilter;
        }
        else
        {
          RandomAccessFilter andFilter = new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{filter,notFilter}));
          filter = andFilter;
        }
      }
      
      return filter;
	}
	
	abstract public RandomAccessFilter buildRandomAccessFilter(String value,Properties selectionProperty) throws IOException;
	
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
	      // there is no hit in this AND filter because this value has no hit
	      return null;
	    }
	  }
	  if (filterList.size() == 0) return null;
	  return new RandomAccessAndFilter(filterList);
	}
	
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
    {
      ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);
      
      for (String val : vals)
      {
        RandomAccessFilter f = buildRandomAccessFilter(val, prop);
        if(f != null && !(f instanceof EmptyFilter)) 
        {
          filterList.add(f);
        }
      }
      
      RandomAccessFilter finalFilter;
      if (filterList.size() == 0)
      {
        finalFilter = EmptyFilter.getInstance();
      }
      else
      {
        finalFilter = new RandomAccessOrFilter(filterList);
      }
      
      if (isNot)
      {
        finalFilter = new RandomAccessNotFilter(finalFilter);
      }
      return finalFilter;
    }
	
	/**
	 * Gets a FacetCountCollector
	 * @param sel selection
	 * @param fspec facetSpec
	 * @return a FacetCountCollector
	 */
	abstract public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec fspec);
	
	/**
	 * Gets the field value
	 * @param id doc
	 * @return array of field values
	 * @see #getFieldValue(int)
	 */
	abstract public String[] getFieldValues(int id);
	
	abstract public Object[] getRawFieldValues(int id);
	
	/**
	 * Gets a single field value
	 * @param id doc
	 * @return first field value
	 * @see #getFieldValues(int)
	 */
	public String getFieldValue(int id)
	{
		return getFieldValues(id)[0];
	}
	
	/**
	 * builds a comparator to determine how sorting is done
	 * @return a sort comparator
	 */
	abstract public ScoreDocComparator getScoreDocComparator();
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}
