/**
 * 
 */
package com.browseengine.bobo.test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanWeight;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.search.BoboSearcher2;
import com.browseengine.bobo.search.FacetHitCollector;

import junit.framework.TestCase;

/**
 * @author xgu
 *
 */
public class BoboSearcher2Test extends TestCase
{

  private static class MyWeight implements Weight {

    public Explanation explain(IndexReader arg0, int arg1) throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }

    public Query getQuery()
    {
      // TODO Auto-generated method stub
      return null;
    }

    public float getValue()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    public void normalize(float arg0)
    {
      // TODO Auto-generated method stub
      
    }

    private static class MyScorer extends Scorer {
      private int _doc = -1;
      private int ptr = -1;
      private int[] fakedata ={5, 6, 7, 8, 9};// has to be sorted in increasing order

      protected MyScorer(Similarity similarity)
      {
        super(similarity);
        // TODO Auto-generated constructor stub
      }

      @Override
      public Explanation explain(int arg0) throws IOException
      {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public float score() throws IOException
      {
        return fakedata[fakedata.length-1] +1- _doc;
      }

      @Override
      public int doc()
      {
        return _doc;
      }

      @Override
      public boolean next() throws IOException
      {
        ptr++;
        if (ptr<fakedata.length) {
          _doc = fakedata[ptr];
          return true;
        }
        return false;
      }

      @Override
      public boolean skipTo(int arg0) throws IOException
      {
        ptr ++;
        if (arg0 <= _doc) arg0 = _doc+1;
        while(ptr<fakedata.length)
        {
          if (fakedata[ptr]>=arg0)
          {
            _doc = fakedata[ptr];
            return true;
          }
          ptr++;
        }
        return false;
      }
      
    }
    public Scorer scorer(IndexReader arg0) throws IOException
    {
      return new MyScorer(org.apache.lucene.search.DefaultSimilarity.getDefault());
    }

    public float sumOfSquaredWeights() throws IOException
    {
      // TODO Auto-generated method stub
      return 0;
    }
    
  }
  private static int[] fakedata ={6, 7, 8, 9};// has to be sorted in increasing order  
  private static class MyIterator extends DocIdSetIterator {
    private int _doc = -1;
    private int ptr = -1;


    @Override
    public int doc()
    {
      return _doc;
    }

    @Override
    public boolean next() throws IOException
    {
      ptr++;
      if (ptr<fakedata.length) {
        _doc = fakedata[ptr];
        return true;
      }
      return false;
    }

    @Override
    public boolean skipTo(int arg0) throws IOException
    {
      ptr ++;
      if (arg0 <= _doc) arg0 = _doc+1;
      while(ptr<fakedata.length)
      {
        if (fakedata[ptr]>=arg0)
        {
          _doc = fakedata[ptr];
          return true;
        }
        ptr++;
      }
      return false;
    }
    
  }
  /**
   * Test method for {@link com.browseengine.bobo.search.BoboSearcher2#search(org.apache.lucene.search.Weight, org.apache.lucene.search.Filter, org.apache.lucene.search.HitCollector)}.
   * @throws IOException 
   */
  public void testSearchWeightFilterHitCollector() throws IOException
  {
    assertEquals(doTestSingle(), doTestList());
  }
  
  private int doTestList() throws IOException {
    BoboSearcher2 bbs2 = new BoboSearcher2(null);
    
    List<FacetHitCollector> facetHitCollectors = new LinkedList<FacetHitCollector>();
    FacetHitCollector o = new FacetHitCollector();
    o._postDocIDSetIterator = new MyIterator();
    RandomAccessDocIdSet radis = new RandomAccessDocIdSet(){

      @Override
      public boolean get(int docId)
      {
        for(int x : fakedata)
        {
          if (x == docId) return true;
        }
        return false;
      }

      @Override
      public DocIdSetIterator iterator()
      {
        // TODO Auto-generated method stub
        return null;
      }};
      

      o._docidSet = radis;
    o._facetCountCollector = new FacetCountCollector(){

      public void collect(int docid)
      {
        // TODO Auto-generated method stub
        
      }

      public void collectAll()
      {
        // TODO Auto-generated method stub
        
      }

      public int[] getCountDistribution()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public String getName()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public BrowseFacet getFacet(String value)
      {
        // TODO Auto-generated method stub
        return null;
      }

      public List<BrowseFacet> getFacets()
      {
        // TODO Auto-generated method stub
        return null;
      }};
      o._docidSet = radis;

      facetHitCollectors.add(o );
    o = new FacetHitCollector();
    o._docidSet = radis;
    o._postDocIDSetIterator = new MyIterator();
    o._facetCountCollector = new FacetCountCollector(){

      public void collect(int docid)
      {
        // TODO Auto-generated method stub
        
      }

      public void collectAll()
      {
        // TODO Auto-generated method stub
        
      }

      public int[] getCountDistribution()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public String getName()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public BrowseFacet getFacet(String value)
      {
        // TODO Auto-generated method stub
        return null;
      }

      public List<BrowseFacet> getFacets()
      {
        // TODO Auto-generated method stub
        return null;
      }};
    facetHitCollectors.add(o );
    bbs2.setFacetHitCollectorList(facetHitCollectors);
    Weight weight = new MyWeight();
    TopDocCollector results = new TopDocCollector(100);
    bbs2.search(weight, null, results);
    return results.getTotalHits();
  }
  
  private int doTestSingle() throws IOException {
    BoboSearcher2 bbs2 = new BoboSearcher2(null);
    
    List<FacetHitCollector> facetHitCollectors = new LinkedList<FacetHitCollector>();
    FacetHitCollector o = new FacetHitCollector();
    o._postDocIDSetIterator = new MyIterator();
    RandomAccessDocIdSet radis = new RandomAccessDocIdSet(){

      @Override
      public boolean get(int docId)
      {
        for(int x : fakedata)
        {
          if (x == docId) return true;
        }
        return false;
      }

      @Override
      public DocIdSetIterator iterator()
      {
        // TODO Auto-generated method stub
        return null;
      }};
      

      o._docidSet = radis;
    o._facetCountCollector = new FacetCountCollector(){

      public void collect(int docid)
      {
        // TODO Auto-generated method stub
        
      }

      public void collectAll()
      {
        // TODO Auto-generated method stub
        
      }

      public int[] getCountDistribution()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public String getName()
      {
        // TODO Auto-generated method stub
        return null;
      }

      public BrowseFacet getFacet(String value)
      {
        // TODO Auto-generated method stub
        return null;
      }

      public List<BrowseFacet> getFacets()
      {
        // TODO Auto-generated method stub
        return null;
      }};
      o._docidSet = radis;

    facetHitCollectors.add(o );
    bbs2.setFacetHitCollectorList(facetHitCollectors);
    Weight weight = new MyWeight();
    TopDocCollector results = new TopDocCollector(100);
    bbs2.search(weight, null, results);
    return results.getTotalHits();
  }

}
