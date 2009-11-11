/**
 * 
 */
package com.browseengine.bobo.docidset;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.search.DocIdSetIterator;

/**
 * OpenBitsetDocSet.
 * A BitsetDocSet that allows more flexibility than BitsetDocSet.
 * 
 * @author spackle
 *
 */
public class OpenBitsetDocSet extends BitsetDocSet {
    protected final BitSet _bs;
    
    /**
     * DO NOT modify <tt>bs</tt> after calling this method, outside of this instance.
     * 
     * @param bs
     */
    public OpenBitsetDocSet(BitSet bs) {
        super();
        _bs = bs;
    }
    
    @Override
    public void addDoc(int docid) {
        _bs.set(docid);
    }

    /**
     * Returns whether or not <tt>docid</tt> is on.
     * true means on.
     * false means off.
     * 
     * @param docid
     * @return
     */
    public boolean get(int docid) {
        return _bs.get(docid);
    }

    @Override
    public int size() {
        return _bs.cardinality();
    }

    @Override
    public DocIdSetIterator iterator() {
        return new MBitsDocIdSetIterator(_bs);
    }

    private static class MBitsDocIdSetIterator extends DocIdSetIterator
    {
        private final BitSet _bs;
        private int _current;
        MBitsDocIdSetIterator(BitSet bs)
        {
            _bs=bs;
            _current=-1;
        }
        
        @Override
        public int doc() {
            return _current;
        }

        @Override
        public boolean next() throws IOException {
            _current=_bs.nextSetBit(_current+1);
            return _current!=-1;
        }

        @Override
        public boolean skipTo(int target) throws IOException {
            _current=_bs.nextSetBit(target);
            return _current!=-1;
        }
        
    }

}
