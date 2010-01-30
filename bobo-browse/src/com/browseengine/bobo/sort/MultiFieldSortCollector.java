package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.OneSortCollector.MyScoreDoc;
import com.browseengine.bobo.util.ListMerger;

public class MultiFieldSortCollector extends SortCollector {
	private final LinkedList<DocIDPriorityQueue> _pqList;
	private final int _numHits;
	private int _totalHits;
	private MyScoreDoc _bottom;
	private MyScoreDoc _tmpScoreDoc;
	private boolean _queueFull;
	private DocComparator[] _currentComparators;
	private DocComparatorSource[] _compSources;
	private DocIDPriorityQueue _currentQueue;
	private MultiDocIdComparator _currentMultiComparator;
	private final boolean _doScoring;
	private float _maxScore;
	private Scorer _scorer;
	private final int _offset;
	private final int _count;
	private BoboIndexReader _currentReader=null;
	private final Map<String,FacetHandler<?>> _facetHandlerMap;
	  
	public MultiFieldSortCollector(DocComparatorSource[] compSources,SortField[] sort,BoboSubBrowser boboBrowser,int offset,int count,boolean doScoring,boolean fetchStoredFields){
		super(sort,fetchStoredFields);
		assert (offset>=0 && count>0);
		_facetHandlerMap = boboBrowser.getFacetHandlerMap();
		_offset = offset;
		_count = count;
		_numHits = _offset+_count;
		_compSources = compSources;
	    _pqList = new LinkedList<DocIDPriorityQueue>();
	    _totalHits = 0;
	    _maxScore = 0.0f;
	    _queueFull = false;
	    _doScoring = doScoring;
	    _currentComparators = new DocComparator[_compSources.length];
	    _currentMultiComparator = new MultiDocIdComparator(_currentComparators);
	    _tmpScoreDoc = new MyScoreDoc();
	}
	
	@Override
	public boolean acceptsDocsOutOfOrder() {
		return _collector == null ? true : _collector.acceptsDocsOutOfOrder();
	}

	@Override
	public void collect(int doc) throws IOException {
		_totalHits++;
		float score;
	    if (_doScoring){
	 	   score = _scorer.score();
	 	   _maxScore+=score;
	    }
	    else{
	 	   score = 0.0f;
	    }
	    if (_queueFull){
	      _tmpScoreDoc.doc=doc;
	      _tmpScoreDoc.score=score;
	      
	      if (_currentMultiComparator.compare(_bottom,_tmpScoreDoc)>=0){
	        return;
	      }
	      MyScoreDoc tmp=_bottom;
	      _bottom = (MyScoreDoc)_currentQueue.replace(_tmpScoreDoc);
	      _tmpScoreDoc = tmp;
	    }
	    else{
	      _bottom = (MyScoreDoc)_currentQueue.add(new MyScoreDoc(doc,score,_currentQueue,_currentReader));
	      _queueFull = (_currentQueue.size() >= _numHits);
	    }
	    
	    if (_collector!=null) _collector.collect(doc);
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		assert reader instanceof BoboIndexReader;
		_currentReader = (BoboIndexReader)reader;

	    for (int i=0;i<_currentComparators.length;++i){
			_currentComparators[i]=_compSources[i].getComparator(reader,docBase);
		}
		_currentQueue = new DocIDPriorityQueue((MultiDocIdComparator)(_currentMultiComparator.clone()),_numHits, docBase);
		_tmpScoreDoc._srcReader = _currentReader;
		_tmpScoreDoc.queue = _currentQueue;
		_pqList.add(_currentQueue);
	    _queueFull = false;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		_scorer = scorer;
		for (DocComparator comparator : _currentComparators){
			comparator.setScorer(scorer);
		}
	}
	
	@Override
	public int getTotalHits(){
	    return _totalHits;
	}

	@Override
	public BrowseHit[] topDocs() throws IOException{
	    ArrayList<Iterator<MyScoreDoc>> iterList = new ArrayList<Iterator<MyScoreDoc>>(_pqList.size());
	    for (DocIDPriorityQueue pq : _pqList){
	      int count = pq.size();
	      MyScoreDoc[] resList = new MyScoreDoc[count];
	      for (int i = count - 1; i >= 0; i--) { 
	    	  resList[i] = (MyScoreDoc)pq.pop();
	      }
	      iterList.add(Arrays.asList(resList).iterator());
	    }
	    
	    ArrayList<MyScoreDoc> resList = ListMerger.mergeLists(_offset, _count, iterList, new Comparator<MyScoreDoc>() {

	        public int compare(MyScoreDoc o1, MyScoreDoc o2) {
	          Comparable s1 = o1.getValue();
	          Comparable s2 = o2.getValue();
	          int r;
	          if (s1 == null) {
	            if (s2 == null) {
	              r = 0;
	            } else {
	              r = -1;
	            }
	          } else if (s2 == null) {
	            r = 1;
	          }
	          else{
	            int v = s1.compareTo(s2);
	            if (v==0){
	              r = o1.doc + o1.queue.base - o2.doc - o2.queue.base;
	            } else {
	              r = v;
	            }
	          }
	          return r;
	        }
	      });
			
	    for (MyScoreDoc doc : resList){
	      doc.doc += doc.queue.base;
	    }
	    return buildHits(resList.toArray(new MyScoreDoc[resList.size()]), _sortFields, _facetHandlerMap, _fetchStoredFields);
	  }

}
