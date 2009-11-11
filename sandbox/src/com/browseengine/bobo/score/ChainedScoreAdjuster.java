package com.browseengine.bobo.score;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.lucene.search.FieldDoc;

public class ChainedScoreAdjuster implements ScoreAdjuster {
	private LinkedList<ScoreAdjuster> _list;
	
	public ChainedScoreAdjuster(){
		_list=new LinkedList<ScoreAdjuster>();
	}
	
	public void addScoreAdjuster(ScoreAdjuster scoreAdjuster){
		synchronized(_list){
			_list.add(scoreAdjuster);
		}
	}
	
	public float adjustScore(int docid, float origScore) {
		Iterator<ScoreAdjuster> iter=_list.iterator();
		float score = origScore;
		while(iter.hasNext()){
			score = iter.next().adjustScore(docid, score);
		}
		return score;		
	}
	
	public FieldDoc adjustScoreDoc(int docid, float origScore) {
		FieldDoc ret = new FieldDoc(docid, origScore);
		return adjustScoreDoc(ret);
	}
	
	public FieldDoc adjustScoreDoc(FieldDoc soFar) {
		Iterator<ScoreAdjuster> iter=_list.iterator();
		while(iter.hasNext()){
			soFar = iter.next().adjustScoreDoc(soFar);
		}
		return soFar;
	}
}
