/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.facets.FacetHandler;


public class SortedHitQueue extends PriorityQueue {
	private static final Logger logger = Logger.getLogger(SortedHitQueue.class);	
	private BoboSubBrowser _boboBrowser;
		
	/** Stores a comparator corresponding to each field being sorted by */
	protected ScoreDocComparator[] comparators;
    Map<String,ScoreDocComparator> _comparatorMap;
    protected boolean[] _isReverse;
    
	public SortedHitQueue(BoboSubBrowser boboBrowser,SortField[] sortFields,int size){
	  _comparatorMap = new HashMap<String,ScoreDocComparator>();
	  _boboBrowser=boboBrowser;
      final int n = sortFields.length;
      ArrayList<ScoreDocComparator> comparatorList = new ArrayList<ScoreDocComparator>(n);
      ArrayList<Boolean> reverseList = new ArrayList<Boolean>(n);
      
      for (int i=0; i<n; ++i) {
        ScoreDocComparator comparator = getScoreDocComparator(sortFields[i]);
        
        if (comparator != null)
        {
          comparatorList.add(comparator);
          reverseList.add(sortFields[i].getReverse());
        }
      }
      this.comparators = comparatorList.toArray(new ScoreDocComparator[comparatorList.size()]);
      _isReverse = new boolean[reverseList.size()];
      int c = 0;
      for (Boolean revVal : reverseList){
        _isReverse[c++] = revVal;
      }
      initialize (size);
	}
	
	/**
	   * Returns whether <code>a</code> is less relevant than <code>b</code>.
	   * @param a ScoreDoc
	   * @param b ScoreDoc
	   * @return <code>true</code> if document <code>a</code> should be sorted after document <code>b</code>.
	   */
	  protected boolean lessThan (final Object a, final Object b) {
	    final ScoreDoc docA = (ScoreDoc) a;
	    final ScoreDoc docB = (ScoreDoc) b;

	    // run comparators
	    int c = 0;
	    int i = 0;
	    for (ScoreDocComparator comparator : comparators) {
	      c = comparator.compare(docA,docB);
	      if (c != 0)
	      {
	        return _isReverse[i] ? c < 0 : c > 0;
	      }
	      i++;
	    }
	    return docA.doc > docB.doc;
	  }

	  public FieldDoc[] getTopDocs(int offset,int numHits)
	  {
	    FieldDoc[] retVal=new FieldDoc[0];
        do{ 
            if (numHits==0) break;
            int size=size();
            if (size==0) break;
            
            if (offset<0 || offset>=size){
                throw new IllegalArgumentException("Invalid offset: "+offset);
            }
                                
            FieldDoc[] fieldDocs=new FieldDoc[size];
            for (int i=size-1;i>=0;i--){
                fieldDocs[i]=(FieldDoc)pop();               
            }
            /*
            if (logger.isDebugEnabled()){
                for (int i=0;i<fieldDocs.length;++i){
                    logger.debug(fieldDocs[i]);
                }
            }
            */
            
            int count=Math.min(numHits,(size-offset));
            retVal=new FieldDoc[count];
            int n=offset+count;
            // if distance is there for 1 hit, it's there for all
            for (int i=offset;i<n;++i){
                FieldDoc hit=fieldDocs[i];
                retVal[i-offset]=hit;
            }
        }while(false);      
        return retVal;
	 }
	  
    ScoreDocComparator getScoreDocComparator(SortField field)
    {
      int type = field.getType();
      if (type == SortField.DOC) return ScoreDocComparator.INDEXORDER;
      if (type == SortField.SCORE) return ScoreDocComparator.RELEVANCE;
      
      String f = field.getField();
      FacetHandler facetHandler = _boboBrowser.getFacetHandler(f);
      ScoreDocComparator comparator = null;
      if (facetHandler != null)
      {
        comparator = facetHandler.getScoreDocComparator();
      }
      if (comparator == null)           // resort to lucene
      {
        try
        {
          comparator = _boboBrowser.getIndexReader().getDefaultScoreDocComparator(field);
        }
        catch(IOException ioe)
        {
          logger.error(ioe.getMessage(),ioe);
        }
      }
      
      if (comparator!=null)
      {
        _comparatorMap.put(f, comparator);
      }
      return comparator; 
    }
}
