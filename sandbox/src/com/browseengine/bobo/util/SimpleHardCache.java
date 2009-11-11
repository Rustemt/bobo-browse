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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 * <or other contact info for bobo-browse; snail mail/email/both>
 */
package com.browseengine.bobo.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A simple hard-reference cache implementation using the LRU algorithm.
 * Uses constant-time operations for lookup, record, and maintenance of 
 * LRU properties.
 * 
 * @author spackle
 *
 */
public class SimpleHardCache<K,V> implements CachingScheme<K,V> {
	private static class LRUList<K,V> {
		private LRUEntry<K,V> head;
		private LRUEntry<K,V> tail;
		private int capacity;
		private int size;
		public LRUList(int cap) {
			head = null;
			tail = null;
			capacity = cap;
			if (capacity <= 1) {
				throw new IllegalArgumentException("no reason to have an LRU cache of size 1 or less");
			}
		}
		public int getCapacity() {
			return capacity;
		}
		public void clear() {
			head = tail = null;
			size = 0;
		}
		/**
		 * assumes: hit came from this LRUList.
		 * @param hit
		 */
		public LRUEntry<K,V> cacheHit(LRUEntry<K,V> hit) {
			if (hit.prev == null) {
				// we are either head, or not in the list
				if (null == head) {
					// there is nothing in the list
					head = tail = hit;
					size++;
				} else if (!hit.key.equals(head.key)) {
					// we are not the head
					// we are not in the list
					// add to head
					head.prev = hit;
					hit.next = head;
					head = hit;
					size++;
				} // else we are already the head
			} else {
				// splice out
				hit.prev.next = hit.next;
				if (null != hit.next) {
					hit.next.prev = hit.prev;
				} else {
					// we were the tail, set a new tail
					tail = hit.prev;
				}
				// go to the head
				// head is non-null since hit.prev is non-null
				head.prev = hit;
				hit.prev = null;
				hit.next = head;
				head = hit;
			}
			return removeIfNeeded();
		}
		/**
		 * assumes: toRemove came from this list, and is in this list now
		 * @param toRemove
		 */
		public void remove(LRUEntry<K,V> toRemove) {
			// splice out
			if (toRemove.prev == null) {
				// toRemove is the head
				this.head = toRemove.next;
			} else {
				toRemove.prev.next = toRemove.next;
			}
			if (toRemove.next == null) {
				// toRemove is the tail
				this.tail = toRemove.prev;
			} else {
				toRemove.next.prev = toRemove.prev;
			}
			// clear references
			toRemove.prev = toRemove.next = null;
			// decrement size
			size--;
		}
		private LRUEntry<K,V> removeIfNeeded() {
			if (size > capacity) {
				// remove from tail
				// we are guaranteed that size >= 2
				tail.prev.next = null;
				LRUEntry<K,V> ret = tail;
				tail = tail.prev;
				ret.prev = null;
				size--;
				return ret;
			}
			return null;
		}
		public LRUEntry<K,V> newEntry(K key, V value) {
			return new LRUEntry<K,V>(key,value);
		}
	}
	static class LRUEntry<K,V> {
		private K key;
		private V value;
		private LRUEntry<K,V> prev;
		private LRUEntry<K,V> next;
		
		public LRUEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}
		
		public V getValue() {
			return value;
		}
		
		public int hashCode() {
			return key.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof LRUEntry) {
				LRUEntry<K,V> other = (LRUEntry<K,V>)o;
				return other.key.equals(this.key);
			}
			return false;
		}
	}
	
	private LRUList<K,V> lru;
	private Map<K,LRUEntry<K,V>> map;
	
	public SimpleHardCache(int maxSize) {
		lru =  new LRUList<K,V>(maxSize);
		map = new Hashtable<K,LRUEntry<K,V>>();
	}
	
	public synchronized V invalidate(K key) {
		LRUEntry<K,V> stored = map.remove(key);
		V val;
		if (stored != null) {
			lru.remove(stored);
			val = stored.value;
		} else {
			val = null;
		}
		return val;
	}
	
	public synchronized V getObject(K key) {
		LRUEntry<K,V> stored = map.get(key);
		V val;
		if (stored != null) {
			lru.cacheHit(stored);
			val = stored.value;
		} else {
			val = null;
		}
		return val;
	}
	
	/**
	 * caches the specified <code>key</code>, <code>value</code> combination.
	 * if <code>key</code> is already stored with a different value, the value is 
	 * replaced by this "new" "fresh" value.
	 * note that if the value came from a cache lookup, it will be the exact 
	 * same object.
	 * returns the (value) just removed, if there was any removed due to 
	 * running out of space in the cache.  note that just letting the return 
	 * value fall out of scope, will not add any references to the key or 
	 * value, so that provided that there are no references elsewhere in the 
	 * code, these objects will get garbage-collected.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public synchronized V cacheObject(K key, V value) {
		LRUEntry<K,V> stored = map.get(key);
		if (stored != null) {
			stored.value = value;
			lru.cacheHit(stored);
		} else {
			LRUEntry<K,V> newEntry = lru.newEntry(key, value);
			map.put(key, newEntry);
			stored = lru.cacheHit(newEntry);
			if (stored != null) {
				map.remove(stored.key);
				return stored.value;
			}
		}
		return null;
	}
	
	/**
	 * used to tell the caller the keys that are currently cached, in unspecified order.  
	 * in a typical cache usage, you never need to call this.  
	 * note that if you iterate through the key set, retrieving values, 
	 * you will preserve all entries in the cache, but will muck up the LRU ordering.  
	 * 
	 * <p>
	 * note that if you modify <code>this</code> at some point after calling this method, 
	 * by calling the {@link #cacheObject(Object, Object)} method at any point, one or 
	 * all of the keys may no longer be valid for this cache set.
	 * 
	 * @return
	 */
	public synchronized Set<K> keySet() {
		Set<K> set = new HashSet<K>();
		set.addAll(map.keySet());
		return set;
	}
	
	/**
	 * Similar to {@link #keySet()}, but ordered by LRU position.
	 * doesn't affect the LRU ordering.
	 * however, if you look up these keys, you can upset the LRU 
	 * ordering.  note that the same caveats apply as above.
	 * You should probably sync lock on your instance, since this 
	 * sort of exposes internals.
	 * 
	 * @return
	 */
	synchronized Iterator<LRUEntry<K,V>> orderedKeysIter() {
		return new LRUIterator<K,V>(lru);
	}
	
	private static class LRUIterator<K,V> implements Iterator<LRUEntry<K,V>> {
		private LRUEntry<K,V> ptr;
		private boolean started;
		public LRUIterator(LRUList<K,V> list) {
			this.ptr = list.head;
			this.started = false;
		}
		public boolean hasNext() {
			if (!started) {
				return ptr != null;
			} else {
				return ptr != null && ptr.next != null;
			}
		}

		public LRUEntry<K, V> next() {
			if (ptr == null) {
				throw new NoSuchElementException("no more");
			}
			LRUEntry<K,V> ret = ptr;
			ptr = ptr.next;
			started = true;
			return ret;
		}

		public void remove() {
			// no implementation for this optional operation
		}
		
	}
	
	public synchronized void clear() {
		lru.clear();
		map.clear();
	}
}
