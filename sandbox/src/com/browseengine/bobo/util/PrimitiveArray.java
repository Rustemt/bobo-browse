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

import java.io.Serializable;
import java.lang.reflect.Array;

public abstract class PrimitiveArray implements Serializable{
	protected Object _array;
	protected int _count;
	private int _growth;
	private Class _type;
	private int _len;
	
	private static final int DEFAULT_SIZE=1000;
	
	protected PrimitiveArray(Class type,int len) {
		super();
		_type=type;
		if (len<=0) throw new IllegalArgumentException("len must be greater than 0: "+len);
		_array=Array.newInstance(type,len);		
		_count=0;
		_growth=10;
		_len=len;
	}
		
	protected PrimitiveArray(Class type){
		this(type,DEFAULT_SIZE);
	}

	protected synchronized void expand(){
		expand(_len+1);
	}
	
	protected synchronized void expand(int idx){
		if (idx<=_len) return;
		int oldLen=_len;
		_len=idx+_growth;
		Object newArray=Array.newInstance(_type,_len);
		System.arraycopy(_array,0,newArray,0,oldLen);
		_growth+=_len;
		_array=newArray;
	}
	
	public synchronized void ensureCapacity(int idx){		
		expand(idx);		
	}
	
	public int size(){
		return _count;
	}

	/**
	 * called to shrink the array size to the current # of elements to save memory.
	 *
	 */
	public synchronized void seal(){
		if (_len > _count){
			Object newArray=Array.newInstance(_type,_count);
			System.arraycopy(_array,0,newArray,0,_count);
			_array=newArray;			
		}
		_growth=10;
	}
	
	@Override
	public PrimitiveArray clone() {
		PrimitiveArray obj;
		try {
			obj = (PrimitiveArray)this.getClass().newInstance();
			obj._count=_count;
			obj._growth=_growth;
			obj._len=_len;
			obj._type=_type;
			
			Object newArray=Array.newInstance(_type,_len);
			System.arraycopy(_array,0,newArray,0,_count);
			obj._array=newArray;
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		} 
	}
	
	public String toString(){
		StringBuffer buffer=new StringBuffer("[");
		for (int i=0;i<_count;++i){
			if (i!=0){
				buffer.append(", ");
			}
			buffer.append(Array.get(_array, i));
		}
		buffer.append(']');
		
		return buffer.toString();
	}
}
