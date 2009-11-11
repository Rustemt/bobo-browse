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

public class FloatArray extends PrimitiveArray{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FloatArray(int len) {
		super(float.class,len);		
	}	
	
	public FloatArray(){
		super(float.class);
	}
	
	public void set(int index,float n){
		ensureCapacity(index);
		Array.setFloat(_array,index,n);
		_count=Math.max(_count, index+1);
	}	
	
	public float get(int index){
		return Array.getFloat(_array, index);
	}
	
	public synchronized void add(float n){		
		ensureCapacity(_count+1);		
		Array.setFloat(_array,_count,n);
		_count++;
	}
	
	public synchronized float[] toArray(){
		float[] ret=new float[_count];
		System.arraycopy(_array,0,ret,0,_count);
		return ret;
	}	
}
