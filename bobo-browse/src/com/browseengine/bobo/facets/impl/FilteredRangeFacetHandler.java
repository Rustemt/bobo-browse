package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;

public class FilteredRangeFacetHandler extends FacetHandler implements FacetHandlerFactory {
    private final List<String> _predefinedRanges;
    private final String _inner;
    private RangeFacetHandler _innerHandler;
    
	public FilteredRangeFacetHandler(String name, String underlyingHandler,List<String> predefinedRanges) {
		super(name, new HashSet<String>(Arrays.asList(underlyingHandler)));
		_predefinedRanges = predefinedRanges;
		_inner = underlyingHandler;
		_innerHandler = null;
	}

	@Override
	public RandomAccessFilter buildRandomAccessFilter(String value,
			Properties selectionProperty) throws IOException {
		return _innerHandler.buildRandomAccessFilter(value, selectionProperty);
	}

	
	@Override
	public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,
			Properties prop) throws IOException {
		return _innerHandler.buildRandomAccessAndFilter(vals, prop);
	}

	@Override
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,
			Properties prop, boolean isNot) throws IOException {
		return _innerHandler.buildRandomAccessOrFilter(vals, prop, isNot);
	}

	@Override
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel,
			FacetSpec fspec) {
		return new RangeFacetCountCollector(_name, _innerHandler.getDataCache(), fspec, _predefinedRanges, false);
	}

	@Override
	public String[] getFieldValues(int id) {
		return _innerHandler.getFieldValues(id);
	}
	
	@Override
	public Object[] getRawFieldValues(int id){
		return _innerHandler.getRawFieldValues(id);
	}

	@Override
	public ScoreDocComparator getScoreDocComparator() {
		return _innerHandler.getScoreDocComparator();
	}

	@Override
	public void load(BoboIndexReader reader) throws IOException {
		FacetHandler handler = reader.getFacetHandler(_inner);
		if (handler instanceof RangeFacetHandler){
			_innerHandler = (RangeFacetHandler)handler;
		}
		else{
			throw new IOException("inner handler is not instance of "+RangeFacetHandler.class);
		}
	}

	public FacetHandler newInstance() {
		return new FilteredRangeFacetHandler(_name,_inner, _predefinedRanges);
	}
}
