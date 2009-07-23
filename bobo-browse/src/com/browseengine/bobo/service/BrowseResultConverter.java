package com.browseengine.bobo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.MappedFacetAccessible;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class BrowseResultConverter implements Converter {

	public void marshal(Object obj, HierarchicalStreamWriter writer,
			MarshallingContext ctx) {
		BrowseResult result=(BrowseResult)obj;
		writer.addAttribute("numhits", String.valueOf(result.getNumHits()));
		writer.addAttribute("totaldocs", String.valueOf(result.getTotalDocs()));
		writer.addAttribute("time", String.valueOf(result.getTime()));
		
		writer.startNode("facets");
		Set<Entry<String,FacetAccessible>> facetAccessors=result.getFacetMap().entrySet();
		
		writer.addAttribute("count", String.valueOf(facetAccessors.size()));
		
		for (Entry<String,FacetAccessible> entry : facetAccessors){
			String choiceName=entry.getKey();
			FacetAccessible facetAccessor = entry.getValue();
			
			List<BrowseFacet> facetList = facetAccessor.getFacets();
			
			writer.startNode("facet");
			writer.addAttribute("name", choiceName);
			
			writer.addAttribute("facetcount", String.valueOf(facetList.size()));
			
			for (BrowseFacet facet : facetList){
				writer.startNode("facet");
				ctx.convertAnother(facet);
				writer.endNode();
			}
			writer.endNode();
		}
		writer.endNode();
		writer.startNode("hits");
		BrowseHit[] hits=result.getHits();

		writer.addAttribute("length", String.valueOf(hits==null ? 0 : hits.length));
		
		for (BrowseHit hit : hits){
			ctx.convertAnother(hit);
		}
		
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext ctx) {		
		BrowseResult res=new BrowseResult();
		
		String numHitsString=reader.getAttribute("numhits");
		if (numHitsString!=null){
			res.setNumHits(Integer.parseInt(numHitsString));
		}
		
		String totalDocsString=reader.getAttribute("totaldocs");
		if (totalDocsString!=null){
			res.setTotalDocs(Integer.parseInt(totalDocsString));
		}
		
		String timeString=reader.getAttribute("time");
		if (timeString!=null){
			res.setTime(Long.parseLong(timeString));
		}
		
		Map<String,FacetAccessible> facetMap = new HashMap<String,FacetAccessible>();
		while (reader.hasMoreChildren()){
			reader.moveDown();
			if ("facets".equals(reader.getNodeName())){
				String facetCountString = reader.getAttribute("count");
				if (facetCountString!=null){
					int count = Integer.parseInt(facetCountString);
					if (count > 0){
						for (int i=0;i<count;++i){
							reader.moveDown();
							String name = reader.getAttribute("name");
							BrowseFacet[] facets = (BrowseFacet[])ctx.convertAnother(res,BrowseFacet[].class);
							facetMap.put(name,new MappedFacetAccessible(facets));
							reader.moveUp();
						}
					}
				}
			}
			else if ("hits".equals(reader.getNodeName())){
				String countStr = reader.getAttribute("length");
				int hitLen = Integer.parseInt(countStr);
				BrowseHit[] hits = new BrowseHit[hitLen];
				for (int i = 0; i< hitLen; ++i){
					reader.moveDown();
					hits[i]=(BrowseHit)ctx.convertAnother(res, BrowseHit.class);
					reader.moveUp();
				}
				res.setHits(hits);
			}
			reader.moveUp();
		}
		return res;
	}

	public boolean canConvert(Class clazz) {
		return BrowseResult.class.equals(clazz);
	}

}
