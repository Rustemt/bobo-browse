package com.browseengine.bobo.sort;

import org.apache.lucene.search.ScoreDoc;


public class MultiDocIdComparator extends DocComparator{
	private DocComparator[] _comparators;
	private int[] _revMult;
	public MultiDocIdComparator(DocComparator[] comparators,boolean[] reverse){
		_comparators = comparators;
		_revMult = new int[reverse.length];
		for (int i=0;i<_revMult.length;++i){
			_revMult[i]=reverse[i] ? -1 : 1;
		}
	}
	
	public int compare(ScoreDoc doc1, ScoreDoc doc2) {
		int v=0;
		for (int i=0;i<_comparators.length;++i){
			v=_comparators[i].compare(doc1, doc2);
			if (v!=0) return (v*_revMult[i]);
		}
		return v;
	}

	@Override
	public Comparable value(final ScoreDoc doc) {
		return new Comparable(){

			public int compareTo(Object o) {
				ScoreDoc otherDoc = (ScoreDoc)o;
				int v=0;
				Comparable c1,c2;
				for (int i=0;i<_comparators.length;++i){
					c1 = _comparators[i].value(doc);
					c2 = _comparators[i].value(otherDoc);
					v = c1.compareTo(c2);
					if (v!=0) {
						v*=_revMult[i];
						break;
					}
				}
				return v;
			}
			
		};
	}
}
