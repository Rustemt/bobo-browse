/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;

/**
 * @author ymatsuda
 *
 */
public abstract class DynamicRangeFacetHandler extends FacetHandler implements FacetHandlerFactory
{
  protected final String _dataFacetName;
  protected RangeFacetHandler _dataFacetHandler;
  
  public DynamicRangeFacetHandler(String name, String dataFacetName)
  {
    super(name,new HashSet<String>(Arrays.asList(new String[]{dataFacetName})));
    _dataFacetName = dataFacetName;
  }
  
  protected abstract String buildRangeString(String val);
  protected abstract List<String> buildAllRangeStrings();
  protected abstract String getValueFromRangeString(String rangeString);
  public abstract FacetHandler newInstance();
  
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String val, Properties props) throws IOException
  {
    return _dataFacetHandler.buildRandomAccessFilter(buildRangeString(val), props);
  }

  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties prop) throws IOException
  {
    List<String> valList = new ArrayList<String>(vals.length);
    for(String val : vals)
    {
      valList.add(buildRangeString(val));
    }
    
    return _dataFacetHandler.buildRandomAccessAndFilter(valList.toArray(new String[valList.size()]), prop);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    List<String> valList = new ArrayList<String>(vals.length);
    for(String val : vals)
    {
      valList.add(buildRangeString(val));
    }
    return _dataFacetHandler.buildRandomAccessOrFilter(valList.toArray(new String[valList.size()]), prop, isNot);
  }

  @Override
  public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec fspec)
  {
    List<String> list = buildAllRangeStrings();
    return new DynamicRangeFacetCountCollector(getName(), _dataFacetHandler, fspec, list);
  }

  @Override
  public String[] getFieldValues(int docid)
  {
    return _dataFacetHandler.getFieldValues(docid);
  }

  @Override
  public ScoreDocComparator getScoreDocComparator()
  {
    return _dataFacetHandler.getScoreDocComparator();
  }

  @Override
  public void load(BoboIndexReader reader) throws IOException
  {
    _dataFacetHandler = (RangeFacetHandler)getDependedFacetHandler(_dataFacetName);
  }
  
  private class DynamicRangeFacetCountCollector extends RangeFacetCountCollector
  {
    DynamicRangeFacetCountCollector(String name, RangeFacetHandler handler, FacetSpec fspec, List<String> predefinedList)
    {
      super(name,handler,fspec,predefinedList,false);
    }

    @Override
    public BrowseFacet getFacet(String value)
    {
      String rangeString = buildRangeString(value);
      BrowseFacet facet = super.getFacet(rangeString);
      if (facet!=null)
      {
        return new BrowseFacet(value,facet.getHitCount());
      }
      else
      {
        return null;
      }
    }

    @Override
    public List<BrowseFacet> getFacets()
    {
      List<BrowseFacet> list = super.getFacets();      
      ArrayList<BrowseFacet> retList = new ArrayList<BrowseFacet>(list.size());
      Iterator<BrowseFacet> iter = list.iterator();
      while(iter.hasNext())
      {
        BrowseFacet facet = iter.next();
        String val = facet.getValue();
        String rangeString = getValueFromRangeString(val);
        if (rangeString != null)
        {
          BrowseFacet convertedFacet = new BrowseFacet(rangeString, facet.getHitCount());
          retList.add(convertedFacet);
        }
      }
      return retList;
    }
  }
}
