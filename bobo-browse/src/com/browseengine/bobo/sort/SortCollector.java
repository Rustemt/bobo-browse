package com.browseengine.bobo.sort;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;

public abstract class SortCollector extends Collector {

	abstract TopDocs topDocs();

	abstract int getTotalHits();
	
	public static SortCollector buildSortCollector(SortField[] sort,int offset,int count,boolean forceScoring){
		boolean doScoring=forceScoring;
		
		for (SortField sf : sort){
			if (sf.getType() == SortField.SCORE) {
				doScoring= true;
			}
			
		}
		
		return null;
	}
}
