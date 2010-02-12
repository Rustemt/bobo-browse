package com.browseengine.bobo.svc.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.gwt.svc.BoboFacetSpec;
import com.browseengine.bobo.gwt.svc.BoboHit;
import com.browseengine.bobo.gwt.svc.BoboRequest;
import com.browseengine.bobo.gwt.svc.BoboResult;
import com.browseengine.bobo.gwt.svc.BoboSearchService;
import com.browseengine.bobo.gwt.svc.BoboSortSpec;
import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.browseengine.bobo.service.BrowseService;

public class BoboSearchServiceImpl implements BoboSearchService {
	private static final Logger log = Logger.getLogger(BoboSearchServiceImpl.class);
	private BrowseService _svc;
	private QueryParser _qparser;
	
	public BoboSearchServiceImpl(BrowseService svc,QueryParser qparser){
		_svc = svc;
		_qparser = qparser;
	}
	
	private static FacetSpec convert(BoboFacetSpec spec){
		FacetSpec fspec = new FacetSpec();
		
		fspec.setMaxCount(spec.getMax());
		fspec.setMinHitCount(spec.getMinCount());
		fspec.setExpandSelection(spec.isExpandSelection());
		fspec.setOrderBy(spec.isOrderByHits() ? FacetSortSpec.OrderHitsDesc : FacetSortSpec.OrderValueAsc);
		return fspec;
	}
	
	private BrowseRequest convert(BoboRequest req) throws Exception{
		BrowseRequest breq = new BrowseRequest();
		String q = req.getQuery();
		if (q!=null){
			breq.setQuery(_qparser.parse(q));
		}
		breq.setOffset(req.getOffset());
		breq.setCount(req.getCount());
		breq.setFetchStoredFields(false);
		List<BoboSortSpec> sortList = req.getSortSpecs();
		int size;
		if (sortList!=null && (size = sortList.size())>0){
		  SortField[] sorts = new SortField[size];
		  int i=0;
		  for (BoboSortSpec sortSpec : sortList){
			  sorts[i++] = new SortField(sortSpec.getField(),sortSpec.isReverse());
		  }
		  breq.setSort(sorts);
		}
		
		Map<String,BoboFacetSpec> facetMap = req.getFacetSpecMap();
		if (facetMap!=null && facetMap.size()>0){
			Set<Entry<String,BoboFacetSpec>> entrySet = facetMap.entrySet();
			for (Entry<String,BoboFacetSpec> entry : entrySet){
				breq.setFacetSpec(entry.getKey(), convert(entry.getValue()));
			}
		}
		return breq;
	}
	
	private static BoboHit convert(BrowseHit hit){
		BoboHit bhit = new BoboHit();
		bhit.setDocid(hit.getDocid());
		bhit.setScore(hit.getScore());
		bhit.setFields(hit.getFieldValues());
		return bhit;
	}
	
	private BoboResult convert(BrowseResult res) throws Exception{
		BoboResult bres = new BoboResult();
		bres.setNumHits(res.getNumHits());
		bres.setTotalDocs(res.getTotalDocs());
		bres.setTime(res.getTime());
		BrowseHit[] bhits = res.getHits();
		List<BoboHit> hits = new ArrayList<BoboHit>(bhits.length);
		for (BrowseHit bhit : bhits){
			hits.add(convert(bhit));
		}
		Map<String,FacetAccessible> facetMap = res.getFacetMap();
		if (facetMap!=null && facetMap.size()>0){
			Map<String,List<FacetValue>> fmap = new HashMap<String,List<FacetValue>>();
			Set<Entry<String,FacetAccessible>> entrySet = facetMap.entrySet();
			for (Entry<String,FacetAccessible> entry : entrySet){
				FacetAccessible accessor = entry.getValue();
				List<BrowseFacet> facetList = accessor.getFacets();
				ArrayList<FacetValue> facetValList = new ArrayList<FacetValue>(facetList.size());
				for (BrowseFacet bfacet : facetList){
					FacetValue fval = new FacetValue(bfacet.getValue(),bfacet.getHitCount());
					facetValList.add(fval);
				}
				fmap.put(entry.getKey(), facetValList);
			}
			bres.setFacetResults(fmap);
		}
		bres.setHits(hits);
		return bres;
	}
	
	public BoboResult search(BoboRequest req) {
		try{
		  BrowseRequest breq = convert(req);
		  BrowseResult bres = _svc.browse(breq);
		  return convert(bres);
		}
		catch(Exception e){
		  log.error(e.getMessage(),e);
		  return null;
		}
	}

	public void close() {
	  try {
		_svc.close();
	  } 
	  catch (BrowseException e) {
		log.error(e.getMessage(),e);
	  }
	}
}
