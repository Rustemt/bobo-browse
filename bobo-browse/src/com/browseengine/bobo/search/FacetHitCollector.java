package com.browseengine.bobo.search;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;


public final class FacetHitCollector{
	
	public FacetCountCollector _facetCountCollector;
	public DocIdSetIterator _postDocIDSetIterator;
	public int _doc;
	public FacetHandler facetHandler;
	public RandomAccessDocIdSet _docidSet=null;
}
