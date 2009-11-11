package org.apache.lucene.search;

import org.apache.lucene.search.FieldCache.StringIndex;
/**
 * 
 */

/**
 * @author kmccrack
 *
 */
public class FieldCacheImpl2 {
    public static class StringIndexGuide extends StringIndex {
        /**
         * the number of values represented by each increment in the lookup
         * array.  note that for large indexes and fields with large number 
         * of values, this value will be > 1.  values <= 1 can be treated 
         * as though you have a raw StringIndex, where 
         * stringIndex.lookup[stringIndex.values[docid]] 
         * is the actual value at that docid.
         */
        public int numPer;
        /**
         * length of full lookup array, if it were in memory.
         * note that this is 1+the number of values you iterate thru in the 
         * TermEnum for this field (the 1 is the 0-index value of null).
         */
        public int numVals;
      public StringIndexGuide(int[] values, String[] lookup, int numPer, int numVals) {
          super(values,lookup);
          this.numPer = numPer;
          this.numVals = numVals;
      }
        
    }

}
