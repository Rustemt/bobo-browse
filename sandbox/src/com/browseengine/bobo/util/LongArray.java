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

package com.browseengine.bobo.util;

import java.lang.reflect.Array;

public class LongArray extends PrimitiveArray {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LongArray(int len) {
		super(long.class,len);		
	}	
	
	public LongArray(){
		super(long.class);
	}
	
	public void set(int index,long n){
		ensureCapacity(index);
		Array.setLong(_array,index,n);
		_count=Math.max(_count, index+1);
	}		
	
	public long get(int index){
		return Array.getLong(_array, index);
	}
	
	public synchronized void add(long n){		
		ensureCapacity(_count+1);			
		Array.setLong(_array,_count,n);
		_count++;
	}
	
	public boolean contains(long elem){
		int size=this.size();
		for (int i=0;i<size;++i){
			if (get(i)==elem) return true;
		}
		return false;
	}
	
	public synchronized long[] toArray(){		
		long[] ret=new long[_count];		
		System.arraycopy(_array,0,ret,0,_count);
		return ret;		
	}
}
