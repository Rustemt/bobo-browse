package com.browseengine.bobo.svc.impl;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.gwt.svc.BoboRequest;
import com.browseengine.bobo.gwt.svc.BoboResult;
import com.browseengine.bobo.gwt.svc.BoboSearchService;
import com.browseengine.bobo.gwt.svc.BoboSortSpec;
import com.browseengine.bobo.service.BrowseService;

public class BoboSearchServiceImpl implements BoboSearchService {
	private static final Logger log = Logger.getLogger(BoboSearchServiceImpl.class);
	private BrowseService _svc;
	private QueryParser _qparser;
	
	public BoboSearchServiceImpl(BrowseService svc,QueryParser qparser){
		_svc = svc;
		_qparser = qparser;
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
		return breq;
	}
	
	private BoboResult convert(BrowseResult res) throws Exception{
		return null;
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
