/**
 * 
 */
package com.browseengine.bobo.docidset;

import org.apache.lucene.search.DocIdSetIterator;

/**
 * @author spackle
 *
 */
public class BitsetRandomAccessDocIdSet extends RandomAccessDocIdSet {
    private OpenBitsetDocSet bitset;
    
    /**
     * Warning: DOES NOT deep copy <tt>bitset</tt>
     * @param bitset the bitset to which <tt>this</tt> should be backed.
     *    You should not modify said bitset after creation.
     */
    public BitsetRandomAccessDocIdSet(OpenBitsetDocSet bitset) {
        this.bitset = bitset;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean get(int docId) {
        return bitset.get(docId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DocIdSetIterator iterator() {
        return bitset.iterator();
    }

}
