/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2009  John Wang
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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 * <or other contact info for bobo-browse; snail mail/email/both>
 */
package com.browseengine.bobo.util;

import java.lang.ref.SoftReference;

/**
 * Uses SoftReference's to the values, and stores them in an 
 * LRU scheme, with constant-time operations.
 * Note that the contract for this type of cache is that the values 
 * can be cleared at any time the SoftReference's object has been cleared.
 * 
 * <p>
 * Hence, in the code block 
 * <code>
 * Key aKey = new Key();
 * Value aValue = new Value();
 * CachingScheme<Key, Value> cache = new CachingScheme<Key, Value>(100);
 * cache.cacheObject(aKey, aValue);
 * Value bStored = cache.getObject(aKey);
 * if (null != bStored) {
 *   // code can get here
 *   System.out.println("got stored value "+bStored);
 * } else {
 *   // code can get here
 *   System.out.println("stored value has been cleared from cache");
 * }
 * </code>
 * both paths are possible.  The reason is that the contract for this cache is that the 
 * reference to a value just stored, can be cleared at any time.
 * 
 * @author spackle
 */
public class SimpleSoftCache<K,V> implements CachingScheme<K,V> {
	private CachingScheme<K,SoftReference<V>> hardCache;
	
	public SimpleSoftCache(int size) {
		hardCache = new SimpleHardCache<K,SoftReference<V>>(size);
	}

	public V cacheObject(K key, V value) {
		SoftReference<V> refVal = new SoftReference<V>(value);
		SoftReference<V> oldVal = hardCache.cacheObject(key, refVal);
		if (null != oldVal) {
			return oldVal.get();
		}
		return null;
	}

	public void clear() {
		hardCache.clear();
	}

	public V getObject(K key) {
		SoftReference<V> foundVal = hardCache.getObject(key);
		if (null != foundVal) {
			return foundVal.get();
		}
		return null;
	}

	public V invalidate(K key) {
		SoftReference<V> invalidVal = hardCache.invalidate(key);
		if (null != invalidVal) {
			return invalidVal.get();
		}
		return null;
	}
	
	
}
