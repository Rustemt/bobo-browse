package com.browseengine.bobo.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;

public class BoboSearcher2 extends BoboSearcher{
    public BoboSearcher2(BoboIndexReader reader)
    {
        super(reader);
    }

    private abstract static class FacetValidator
    {
      protected final FacetHitCollector[] _collectors;
      protected final FacetCountCollector[] _countCollectors;
      protected final int _numPostFilters;
      public int _nextTarget;
      
      public FacetValidator(FacetHitCollector[] collectors,FacetCountCollector[] countCollectors,int numPostFilters) throws IOException
      {
        _collectors = collectors;
        _countCollectors = countCollectors;
        _numPostFilters = numPostFilters;
      }
      /**
       * This method validates the doc against any multi-select enabled fields.
       * @param docid
       * @return true if all fields matched
       */
      public abstract boolean validate(final int docid)
        throws IOException;
      
    }
    
    private final static class DefaultFacetValidator extends FacetValidator{
    	
    	public DefaultFacetValidator(FacetHitCollector[] collectors,FacetCountCollector[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    	}
    	 /**
         * This method validates the doc against any multi-select enabled fields.
         * @param docid
         * @return true if all fields matched
         */
    	@Override
        public final boolean validate(final int docid)
          throws IOException
        {
          FacetHitCollector miss = null;
          
          for(int i = 0; i < _numPostFilters; i++)
          {
            FacetHitCollector facetCollector = _collectors[i];
            if(facetCollector._more)
            {
              int sid = facetCollector._doc;
              if(sid == docid) continue; // matched
              
              if(sid < docid)
              {
                DocIdSetIterator iterator = facetCollector._postDocIDSetIterator;
                if(iterator.skipTo(docid))
                {
                  sid = iterator.doc();
                  facetCollector._doc = sid;
                  if(sid == docid) continue; // matched
                }
                else
                {
                  facetCollector._more = false;
                  facetCollector._doc = Integer.MAX_VALUE;
                  
                  // move this to front so that the call can find the failure faster
                  FacetHitCollector tmp = _collectors[0];
                  _collectors[0] = facetCollector;
                  _collectors[i] = tmp;
                }
              }  
            }
            
            if(miss != null)
            {
              // failed because we already have a mismatch
              _nextTarget = (miss._doc < facetCollector._doc ? miss._doc : facetCollector._doc);
              return false;
            }
            miss = facetCollector;
          }
          
          _nextTarget = docid + 1;

          if(miss != null)
          {
            miss._facetCountCollector.collect(docid);
            return false;
          }
          else
          {
            for (FacetCountCollector collector : _countCollectors)
            {
          	 collector.collect(docid);
            }
            return true;
          }
        }
    }
    
    private final static class OnePostFilterFacetValidator extends FacetValidator{
    	private FacetHitCollector _firsttime;
    	OnePostFilterFacetValidator(FacetHitCollector[] collectors,FacetCountCollector[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    		_firsttime = _collectors[0];
    	}

		@Override
		public final boolean validate(int docid) throws IOException {
			FacetHitCollector miss = null;
			
            RandomAccessDocIdSet set = _firsttime._docidSet;
            if (set!=null && !set.get(docid))
            {
              miss = _firsttime;
            }
            _nextTarget = docid + 1;

            if(miss != null)
            {
              miss._facetCountCollector.collect(docid);
              return false;
            }
            else
            {
              for (FacetCountCollector collector : _countCollectors)
              {
            	 collector.collect(docid);
              }
              return true;
            }
		}
    }
    
    private final static class NoNeedFacetValidator extends FacetValidator{
    	NoNeedFacetValidator(FacetHitCollector[] collectors,FacetCountCollector[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    	}

		@Override
		public final boolean validate(int docid) throws IOException {
			for (FacetCountCollector collector : _countCollectors)
            {
            	collector.collect(docid);
            }
            return true;
		}
    	
    }
    
    protected FacetValidator createFacetValidator() throws IOException
    {
    	
    	FacetHitCollector[] collectors = new FacetHitCollector[_facetCollectors.size()];
    	FacetCountCollector[] countCollectors = new FacetCountCollector[collectors.length];
        int numPostFilters;
        int i = 0;
        int j = collectors.length;
        
        for (FacetHitCollector facetCollector : _facetCollectors)
        {
          if (facetCollector._postDocIDSetIterator != null) 
          {
            facetCollector._more = facetCollector._postDocIDSetIterator.next();
            facetCollector._doc = (facetCollector._more ? facetCollector._postDocIDSetIterator.doc() : Integer.MAX_VALUE);
            collectors[i] = facetCollector;
            countCollectors[i]=facetCollector._facetCountCollector;
            i++;
          }
          else
          {
            j--;
            collectors[j] = facetCollector;
            countCollectors[j] = facetCollector._facetCountCollector;
          }
        }
        numPostFilters = i;

      if(numPostFilters == 0){
        return new NoNeedFacetValidator(collectors,countCollectors,numPostFilters);
      }
      else if (numPostFilters==1){
    	return new OnePostFilterFacetValidator(collectors,countCollectors,numPostFilters);  
      }
      else{
        return new DefaultFacetValidator(collectors,countCollectors,numPostFilters);
      }
    }
    
    @Override
    public void search(Weight weight, Filter filter, HitCollector results)
            throws IOException {
        IndexReader reader=getIndexReader();
        
        Scorer scorer = weight.scorer(reader);
        if (scorer == null)
          return;

        final FacetValidator validator = createFacetValidator();
        int target = 0;
        boolean more;
        
        if (filter == null)
        {
          more = scorer.next();
          while(more)
          {
            target = scorer.doc();
            if(validator.validate(target))
            {
              results.collect(target, scorer.score());
              more = scorer.next();
            }
            else
            {
              target = validator._nextTarget;
              more = scorer.skipTo(target);
            }
          }
          return;
        }

        DocIdSetIterator filterDocIdIterator = filter.getDocIdSet(reader).iterator(); // CHECKME: use ConjunctionScorer here?
        
        if(!filterDocIdIterator.next()) return;
        target = filterDocIdIterator.doc();
        
        int doc = -1;
        while(true)
        {
          if(doc < target)
          {
            if(!scorer.skipTo(target)) break;
            doc = scorer.doc();
          }
          
          if(doc == target) // permitted by filter
          {
            if(validator.validate(doc))
            {
              results.collect(doc, scorer.score());
              
              if(!filterDocIdIterator.next()) break;
              target = filterDocIdIterator.doc();
              continue;
            }
            else
            {
              // skip to the next possible docid
              target = validator._nextTarget;
            }
          }
          else // doc > target
          {
            target = doc;
          }
          
          if(!filterDocIdIterator.skipTo(target)) break;
          target = filterDocIdIterator.doc();
        }
    }
}
