package com.browseengine.bobo.facets.data;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;

public class FacetDataCacheFieldComparator extends FieldComparator {
    private FacetDataCache _dataCache;
    private final String _fieldname;
    private final int _sortPos;
    private boolean _reversed;
    
    private int currentReaderGen = -1;
    
    private final int[] ords;
    private final String[] values;
    private final int[] readerGen;
    
    private int bottomSlot = -1;
    private int bottomOrd;
    private String bottomValue;
    
	public FacetDataCacheFieldComparator(String fieldname,int numHits, int sortPos, boolean reversed) {
		_dataCache = null;
		_fieldname = fieldname;
		_sortPos = sortPos;
		_reversed = reversed;
		ords = new int[numHits];
		values = new String[numHits];
		readerGen = new int[numHits];
	}

	@Override
	public int compare(int slot1, int slot2) {
	  if (readerGen[slot1] == readerGen[slot2]) {
        int cmp = ords[slot1] - ords[slot2];
        if (cmp != 0) {
          return cmp;
        }
      }

      final String val1 = values[slot1];
      final String val2 = values[slot2];
      if (val1 == null) {
        if (val2 == null) {
          return 0;
        }
        return -1;
      } else if (val2 == null) {
        return 1;
      }
      return val1.compareTo(val2);
	}

	@Override
	public int compareBottom(int doc) throws IOException {
	  int order = _dataCache.orderArray.get(doc);
      final int cmp = bottomOrd - order;
      if (cmp != 0) {
        return cmp;
      }

      final String val2 = _dataCache.valArray.get(order);
      if (bottomValue == null) {
        if (val2 == null) {
          return 0;
        }
        // bottom wins
        return -1;
      } else if (val2 == null) {
        // doc wins
        return 1;
      }
      return bottomValue.compareTo(val2);
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		final int ord = _dataCache.orderArray.get(doc);
	    ords[slot] = ord;
	    values[slot] = _dataCache.valArray.get(ord);
	    readerGen[slot] = currentReaderGen;
	}
	
	private void convert(int slot) {
      readerGen[slot] = currentReaderGen;
      int index = 0;
      String value = values[slot];
      if (value == null) {
        ords[slot] = 0;
        return;
      }

      if (_sortPos == 0 && bottomSlot != -1 && bottomSlot != slot) {
        // Since we are the primary sort, the entries in the
        // queue are bounded by bottomOrd:
        if (_reversed) {
          //index = binarySearch(lookup, value, bottomOrd, lookup.length-1);
          index = _dataCache.valArray.indexOf(value);
        } else {
          // index = binarySearch(lookup, value, 0, bottomOrd);
          index = _dataCache.valArray.indexOf(value);
        }
      } else {
        // Full binary search
        index = _dataCache.valArray.indexOf(value);
      }

      if (index < 0) {
        index = -index - 2;
      }
      ords[slot] = index;
    }

	@Override
	public void setBottom(int bottom) {
	  bottomSlot = bottom;
      if (readerGen[bottom] != currentReaderGen) {
        convert(bottomSlot);
      }
      bottomOrd = ords[bottom];
      bottomValue = values[bottom];
	}

	@Override
	public void setNextReader(IndexReader reader, int docbase) throws IOException {
		if (reader instanceof BoboIndexReader){
			BoboIndexReader boboReader = (BoboIndexReader)reader;
			FacetHandler fhandler= boboReader.getFacetHandler(_fieldname);
			if (fhandler instanceof FacetDataCacheSource){
				_dataCache = ((FacetDataCacheSource)fhandler).getDataCache();
				currentReaderGen++;
			    if (bottomSlot != -1) {
			      convert(bottomSlot);
			      bottomOrd = ords[bottomSlot];
			    }
			}
			else{
				new IllegalStateException("handler is not instanceof "+FacetDataCacheSource.class);
			}
		}
		else{
			throw new IllegalStateException("reader is not instanceof "+BoboIndexReader.class.getName());
		}
	}

	@Override
	public Comparable value(int slot) {
		 return values[slot];
	}

}
