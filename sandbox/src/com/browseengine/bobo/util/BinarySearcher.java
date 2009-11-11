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

public class BinarySearcher {
	public static enum BinarySearchType{
		NONE_IF_NOT_FOUND,
		FIRST_BEFORE_IF_NOT_FOUND,
		FIRST_AFTER_IF_NOT_FOUND
	}
	
	public static int binarySearch(Comparable item,int startIndex,int endIndex,Object array,BinarySearchType type){       
        while (endIndex > startIndex){
            int mid=(endIndex-startIndex)/2+startIndex;                 
            
            if (Array.get(array, mid)==null){
            	startIndex=mid+1;
            	continue;
            }
            if (item.compareTo(Array.get(array, mid))>0){
                startIndex=mid+1;
                continue;
            }
            else if (item.compareTo(Array.get(array, mid))<0){
                endIndex=mid;
                continue;               
            }
            else {
                return mid;
            }       
        }
        
        if (type==BinarySearchType.NONE_IF_NOT_FOUND){
        	return -1;
        }
        else if (type==BinarySearchType.FIRST_BEFORE_IF_NOT_FOUND){
        	if (startIndex == endIndex)
                return Math.max(0,startIndex-1);
            return 0;
        }
        else{
        	int len=Array.getLength(array);
        	if (startIndex == endIndex)
                return Math.min(startIndex,len-1);
            return (len-1);
        }
        
    }
	
	public static int binarySearch(Comparable item,Object array,BinarySearchType type){
		return binarySearch(item,0,Array.getLength(array),array,type);
	}
	
	public static void main(String[] args) {
		String[] sarray=new String[]{"a","b","c","d","e","f","g","u","v","w","x","y","z"};
		
		System.out.println(BinarySearcher.binarySearch("za",sarray,BinarySearchType.FIRST_BEFORE_IF_NOT_FOUND));
		int[] a1=new int[]{5,6};
		Comparable comp=(Comparable)Array.get(a1, 0);
		Comparable c2=(Comparable)Array.get(a1, 1);;
		
		System.out.println(comp.compareTo(c2));
	}
}
