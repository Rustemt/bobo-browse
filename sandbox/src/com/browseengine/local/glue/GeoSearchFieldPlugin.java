/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * contact owner@browseengine.com.
 */

package com.browseengine.local.glue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDocComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessOrFilter;
import com.browseengine.local.service.geosearch.GeoSearchImpl;
import com.browseengine.local.service.geosearch.GeoSearchingException;
import com.browseengine.local.service.index.GeoSearchFields;

/**
 * The GeoSearchField is a virtual field that is actually the concatenation of two valid 
 * Lucene fields in the index, separated by an underscore '_' character.  The fields themselves 
 * should not contain underscore.  Each field must contain an indexed single entry for each doc, 
 * and the field values must be numeric [parseable by Double.parseDouble(String)].  The 
 * longitudinal field name should appear first, followed by the latitudinal field name.
 * 
 * @author spackle
 *
 */
public class GeoSearchFieldPlugin extends FacetHandler {
	private static final Logger LOGGER = Logger.getLogger(GeoSearchFieldPlugin.class);	
	private static final String FIELD_TYPE = "geosearch";
	
	private GeoPluginFieldData _lonLats = null;
	
	/**
	 * There is no documentation on what on earth a ChoiceCollection is.
	 * Is it a misnomer for something else?
	 * 
	 * @author spackle
	 *
	 */
	/*
	private static class GeoSearchCollection extends ChoiceCollection{
		private GeoPluginFieldData _geoData;
		public GeoSearchCollection(String name, GeoPluginFieldData data) {
			super(name,new int[1]);
			_geoData = data;
		}
		
		@Override
		public void collect(int docid) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void fillChoiceCollection(ChoiceCollector collector, BrowseSelection origSelection, OutputSpec outputSpec) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getChoiceCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ChoiceContainer getTopChoices(BrowseSelection origSelection, OutputSpec outputSpec) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getValue(int docid) {
			return new StringBuilder().append('{').append(_geoData.lons[docid]).append(',').append(_geoData.lats[docid]).append('}').toString();
		}

		@Override
		public String[] getValues(int docid) {
			String val = getValue(docid);
			if (val != null) {
				return new String[]{val};
			}
			return new String[0];
		}
	}
	*/
	
	/**
	 * Returns a dummy facet count collector.
	 * May be a place to add dividing into distance buckets in the future.
	 * 
	 * {@inheritDoc}
	 */
	public FacetCountCollector getFacetCountCollector(BrowseSelection sel, FacetSpec fspec) {
	    return new GeoFacetCountCollector(getFieldName());
	}
	
	private static class GeoFacetCountCollector implements FacetCountCollector {
	    private String name;
	    public GeoFacetCountCollector(String name) {
	        this.name = name;
	    }
        /**
         * {@inheritDoc}
         */
        public void collect(int docid) {
            // TODO Auto-generated method stub
            
        }

        /**
         * {@inheritDoc}
         */
        public void collectAll() {
            // TODO Auto-generated method stub
            
        }

        /**
         * {@inheritDoc}
         */
        public int[] getCountDistribution() {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public BrowseFacet getFacet(String value) {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public List<BrowseFacet> getFacets() {
            // TODO Auto-generated method stub
            return null;
        }
	    
	}
	
	static class GeoPluginFieldData implements Serializable{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		GeoPluginFieldData(IndexReader reader, String fldName) throws IOException, GeoSearchingException {
			Pattern p = Pattern.compile("\\A([^_]*)_([^_]*)\\z");
			Matcher m = p.matcher(fldName);
			if (!m.matches()) {
				throw new GeoSearchingException("bad field name '"+fldName+"' was not two field names concatenated by a '_' (underscore) character");
			}
			String lonFld = m.group(1);
			String latFld = m.group(2);
			lons = GeoSearchImpl.loadDegreeFieldIntoInt(reader, lonFld);
			lats = GeoSearchImpl.loadDegreeFieldIntoInt(reader, latFld);
			fieldName = fldName;
		}
		
		String fieldName;
		int[] lons;
		int[] lats;
		
		String getLatitudeString(int docid) {
		    int lat = lats[docid];
		    return toDoubleToString(lat);
		}
		String getLongitudeString(int docid) {
		    int lon = lons[docid];
		    return toDoubleToString(lon);
		}
		private String toDoubleToString(int lonOrLatAsInt) {
		    return String.valueOf(GeoSearchFields.intToDub(lonOrLatAsInt));
		}
	}

	public GeoSearchFieldPlugin(String fieldName) {
	    super(fieldName);
	}
	
	/**
	 * Use cautiously.  okay to use for reading, but don't modify it.
	 * 
	 * @return
	 */
	GeoPluginFieldData getGeoPluginFieldData() {
	    return _lonLats;
	}
	
	public RandomAccessFilter buildRandomAccessFilter(String value, Properties selectionProperty) throws IOException {
	    String[] vals = {
	            value
	    };
	    final String fieldName = getFieldName();
	    return buildFilters(fieldName, vals);
	}

	public RandomAccessFilter buildFilters(String fieldname, String[] vals) {
		if (_lonLats == null) {
			throw new IllegalStateException("you must initialize a "+getClass()+
					" before getting filters from it, by first calling loadFieldDataCache!");
		}
		GeoSearchSelection[] sels = GeoSearchSelection.parse(vals);
		if (sels == null) {
			StringBuilder buf = new StringBuilder().
			  append("bad argument list for geo search field plugin, field: '").
			  append(fieldname).append("', values: ");
			if (vals == null || vals.length == 0) {
				buf.append("none");
			} else {
				buf.append("{'").append(vals[0]).append('\'');
				for (int i = 1; i < vals.length; i++) {
					buf.append(", '").append(vals[i]).append("'");
				}
				buf.append('}');
			}
			LOGGER.warn(buf.toString());
		}
		RandomAccessFilter randomAccessFilter;
		if (null != sels && sels.length == 1) {
		    randomAccessFilter = getGeoSearchFilter(sels[0]);
		} else {
		    int count = null != sels ? sels.length : 0;
		    List<RandomAccessFilter> geoFilters = new ArrayList<RandomAccessFilter>(sels.length);
		    for (int i =  0; i < count; i++) {
		        geoFilters.add(getGeoSearchFilter(sels[i]));
		    }
		    randomAccessFilter = new RandomAccessOrFilter(geoFilters);
		}
		return randomAccessFilter;
	}
	
	private RandomAccessFilter getGeoSearchFilter(GeoSearchSelection sel) {
	    GeoSearchFilter geoSearchFilter = new GeoSearchFilter(_lonLats, sel.getLon(), sel.getLat(), sel.getRangeInMiles());
	    return geoSearchFilter;
	}

	public BrowseSelection buildSelection(String fieldname, String[] values) {
		if (values!=null && values.length>2){
			BrowseSelection sel=new BrowseSelection(fieldname);
			sel.addValue(values[0]);
			sel.addValue(values[1]);
			sel.addValue(values[2]);
			return sel;
		}
		else{
			return null;
		}
	}

	@Override
	public ScoreDocComparator getScoreDocComparator() {
        // there is no pre-defined geographic sort order; it is different for each search context
		return null;
		/*
		Object userData=fieldDataCache.getUserObject();
		if (userData instanceof GeoPluginFieldData){
			return new GeoSearchDocComparator((GeoPluginFieldData)userData, 0d, 0d);
		}
		else{
			return null;
		}
		*/
	}

	public static String getTypeString() {
		return FIELD_TYPE;
	}
	
	private String getFieldName() {
	    return getName();
	}
	
	public String[] getFieldValues(int docid) {
	    return new String[] {
	        _lonLats.getLongitudeString(docid)+","+_lonLats.getLatitudeString(docid)
	    };
	}
	
	public Object[] getRawFieldValues(int docid) {
	    return getFieldValues(docid);
	}

	/**
	 * this method _must_ be called first, before building any filters with this.
	 * we should be okay, since we are called by BoboIndexReader constructor.
	 */
	public void load(BoboIndexReader reader) throws IOException {
		try {
			_lonLats=new GeoPluginFieldData(reader,getFieldName());
//			fieldDataCache.setUserObject(_lonLats);
		} catch (GeoSearchingException gse) {
			throw new IOException("trouble loading field data cache: "+gse.toString());
		}
	}

    /*
	@Override
	public ChoiceCollection newCollection(FieldDataCache fieldDataCache) {
		GeoPluginFieldData data=(GeoPluginFieldData)fieldDataCache.getUserObject();
		ChoiceCollection coll = new GeoSearchCollection(fieldDataCache.getFieldName(), data);
		return coll;
	}

	@Override
	public String normalize(String rawValue) {
		// TODO: there is a problem with populateDocument(), which calls this method.
		// namely, this is a hyperfield, and we can't just use <Lucene>doc.getValues() 
		// for this hyperfield.  instead, we need to figure out how we can 
		// prevent that method from being called.
		return rawValue.trim().toLowerCase();
	}

	@Override
	public void preloadCache(Map<String, BitSet> cache, String fieldname, String val, BoboIndexReader reader) throws IOException {
		// no preloaded results--we don't know where the user is gonna search from
	}

	/ **
	 * probably pointless to support NOT operator to find things "far away".
	 * hence, we return <code>false</code>.
	 * /
	public boolean supportNotSelections() {
		return false;
	}

	@Override
	public String[] tokenize(String rawValue) {
		return new String[]{normalize(rawValue)};
	}

	@Override
	public ChoiceContainer mergeChoices(ChoiceContainer[] choiceContainers, OutputSpec ospec) {
		// TODO Auto-generated method stub
		return null;
	}
    */
}
