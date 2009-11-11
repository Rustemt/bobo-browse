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

package com.browseengine.bobo.score;

import org.apache.lucene.search.FieldDoc;

public interface ScoreAdjuster {
	/**
	 * This method will just get the raw adjusted score back.
	 * 
	 * @param docid
	 * @param origScore
	 * @return
	 */
	float adjustScore(int docid, float origScore);
	/**
	 * This method will get the raw adjusted score back, plus 
	 * possibly some extra data about the components of the 
	 * score, that we might care about in client display.  
	 * For example, for distance search, the chaining of scores 
	 * might produce a final score, but we want to show the user 
	 * how far away that hit is from the centroid of the search.
	 * 
	 * @param docid
	 * @param origScore
	 * @return
	 */
	FieldDoc adjustScoreDoc(int docid,float origScore);
	/**
	 * Same as {@link #adjustScoreDoc(int, float)}, but used when 
	 * we already have a <code>FieldDoc</code> instance, as in 
	 * chaining multiple score adjusters together in tandem.
	 * 
	 * @param docid
	 * @return
	 */
	FieldDoc adjustScoreDoc(FieldDoc docid);
}
