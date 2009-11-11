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

public class IntArray extends PrimitiveArray{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IntArray(int len) {
		super(int.class,len);		
	}	
	
	public IntArray(){
		super(int.class);
	}	
	
	public synchronized void add(int n){		
		ensureCapacity(_count+1);		
		Array.setInt(_array,_count,n);
		_count++;
	}
	
	public boolean contains(int elem){
		int size=this.size();
		for (int i=0;i<size;++i){
			if (get(i)==elem) return true;
		}
		return false;
	}
	
	public void set(int index,int n){
		ensureCapacity(index);
		Array.setInt(_array,index,n);
		_count=Math.max(_count, index+1);
	}
	
	public int get(int index){
		return Array.getInt(_array, index);
	}
	
	public synchronized int[] toArray(){		
		int[] ret=new int[_count];		
		System.arraycopy(_array,0,ret,0,_count);
		return ret;		
	}
	
	public static void main(String[] args) {
		IntArray array=new IntArray(2);
		for (int i=0;i<5;++i){
			array.add(i);
		}
		System.out.println(array);
		array.set(100, 13);
		System.out.println(array);
		array.seal();
		System.out.println(array);		
	}
}
