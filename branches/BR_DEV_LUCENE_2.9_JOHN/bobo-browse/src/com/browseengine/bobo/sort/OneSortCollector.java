package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.util.ListMerger;

public class OneSortCollector extends SortCollector {
  private final LinkedList<DocIDPriorityQueue> _pqList;
  private final int _numHits;
  private int _totalHits;
  private MyScoreDoc _bottom;
  private final boolean _reverse;
  private boolean _queueFull;
  private DocComparator _currentComparator;
  private DocComparatorSource _compSource;
  private DocIDPriorityQueue _currentQueue;
  private BoboIndexReader _currentReader=null;
  private MyScoreDoc _tmpDoc;
  
  private final boolean _doScoring;
  private float _maxScore;
  private Scorer _scorer;
  private int _offset;
  private int _count;
  
  private final Map<String,FacetHandler<?>> _facetHandlerMap;
	
  static class MyScoreDoc extends ScoreDoc {
    DocIDPriorityQueue queue;
    BoboIndexReader _srcReader;
    public MyScoreDoc(){
    	this(0,0.0f,null,null);
    }
    
    public MyScoreDoc(int docid, float score, DocIDPriorityQueue queue,BoboIndexReader reader) {
      super(docid, score);
      this.queue = queue;
      _srcReader = reader;
    }
    
    Comparable getValue(){
    	return queue.sortValue(this);
    }
  }
	
  public OneSortCollector(DocComparatorSource compSource,SortField[] sortFields,BoboSubBrowser boboBrowser,int offset,int count,boolean doScoring,boolean fetchStoredFields) {
	super(sortFields,fetchStoredFields);
	_facetHandlerMap = boboBrowser.getFacetHandlerMap();
    _compSource = compSource;
    _pqList = new LinkedList<DocIDPriorityQueue>();
    assert (offset>=0 && count>0);
    _numHits = offset + count;
    _offset = offset;
    _count = count;
    _totalHits = 0;
    _queueFull = false;
    _reverse = compSource.isReverse();
    _doScoring = doScoring;
    _tmpDoc = new MyScoreDoc();
    _maxScore = 0.0f;
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
      _tmpDoc.doc = doc;
      _tmpDoc.score = score;
      int v = _currentComparator.compare(_bottom,_tmpDoc);
      if (v==0 || ((v<0) && !_reverse)){
        return;
      }
      MyScoreDoc tmp = _bottom;
      _bottom = (MyScoreDoc)_currentQueue.replace(_tmpDoc);
      _tmpDoc = tmp;
    }
    else{ 
      _bottom = (MyScoreDoc)_currentQueue.add(new MyScoreDoc(doc,score,_currentQueue,_currentReader));
      _queueFull = (_currentQueue.size() >= _numHits);
    }
    
    if (_collector != null) _collector.collect(doc);
  }

  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
	assert reader instanceof BoboIndexReader;
	_currentReader = (BoboIndexReader)reader;
    _currentComparator = _compSource.getComparator(reader,docBase);
    _currentQueue = new DocIDPriorityQueue(_currentComparator,
                                           _numHits, docBase);

    _tmpDoc.queue=_currentQueue;
    _tmpDoc._srcReader = _currentReader;
    _pqList.add(_currentQueue);
    _queueFull = false;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
	  _scorer = scorer;
	  _currentComparator.setScorer(scorer);
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
    
    final int revMult = _reverse ? -1 : 1;
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
          
          return revMult * r;
        }
      });
		
    for (MyScoreDoc doc : resList){
      doc.doc += doc.queue.base;
    }
    
    return buildHits(resList.toArray(new MyScoreDoc[resList.size()]), _sortFields, _facetHandlerMap, _fetchStoredFields);
  }
}
