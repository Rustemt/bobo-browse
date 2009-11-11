/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
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
 * contact owner@browseengine.com.
 */

package com.browseengine.bobo.util;

import java.util.Iterator;
import java.util.Set;

import com.browseengine.bobo.util.SimpleHardCache.LRUEntry;

/**
 * @author spackle
 *
 */
public class TimeoutHardCache<K,V> implements CachingScheme<K,V> {
	private long timeoutInterval;
	private SimpleHardCache<K,TimeoutValue<V>> cache;
	private static class TimeoutValue<V> {
		private V externalValue;
		private transient long timeoutStamp;
		public TimeoutValue(V val, long timeoutStamp) {
			this.externalValue = val;
			this.timeoutStamp = timeoutStamp;
		}
	}
	public TimeoutHardCache(int maxSize, long timeoutMillis) {
		cache = new SimpleHardCache<K,TimeoutValue<V>>(maxSize);
		this.timeoutInterval = timeoutMillis;
	}
	public V invalidate(K key) {
		long now = System.currentTimeMillis();
		TimeoutValue<V> tmp = cache.invalidate(key);
		if (tmp != null && now <= tmp.timeoutStamp) {
			// if it's timed out, you can't see it!
			return tmp.externalValue;
		}
		return null;
	}
	public V cacheObject(K key, V value) {
		// we are being told to cache this new value; reset the timeout
		long now = System.currentTimeMillis();
		TimeoutValue<V> mine = new TimeoutValue<V>(value, now+this.timeoutInterval);
		TimeoutValue<V> old = cache.cacheObject(key, mine);
		if (old != null && now <= old.timeoutStamp) {
			// if it's timed out, you can't see it!
			return old.externalValue;
		}
		return null;
	}
	public V getObject(K key) {
		long now = System.currentTimeMillis();
		TimeoutValue<V> retrieved = cache.getObject(key);
		// if it's timed out, you can't see it!
		// fake it.
		// TODO: erase it from the cache; possibly from within a synchronized block
		if (retrieved != null && now <= retrieved.timeoutStamp) {
			return retrieved.externalValue;
		}
		return null;
	}
	public Set<K> keySet() {
		return cache.keySet();
	}
	public void clear() {
		cache.clear();
	}
	/**
	 * mucks up the LRU ordering, to invalidate everything that's timed out.
	 * a potentially expensive, linear in the size of the cache, operation.
	 *
	 */
	public void invalidateTimeouts() {
		long now = System.currentTimeMillis();
		synchronized(cache) {
			Iterator<LRUEntry<K,TimeoutValue<V>>> iter = cache.orderedKeysIter();
			while (iter.hasNext()) {
				LRUEntry<K,TimeoutValue<V>> entry = iter.next();
				if (now > entry.getValue().timeoutStamp) {
					invalidate(entry.getKey());
				}
			}
		}
	}
}
