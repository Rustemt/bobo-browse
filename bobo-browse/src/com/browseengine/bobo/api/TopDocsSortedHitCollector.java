package com.browseengine.bobo.api;

import java.io.IOException;

import org.apache.lucene.search.Collector;

public abstract class TopDocsSortedHitCollector extends Collector
{

  @Override
  abstract public void collect(int doc) throws IOException;
  
  abstract public BrowseHit[] getTopDocs() throws IOException;
  
  abstract public int getTotalHits();  
}
