# Introduction #

When performing a browse on a set of documents with facets specified to be returned, it is desirable to be able to specify the order the facet values are to be returned.

### Default Facet Sort Specs ###

The Bobo Browse api by default supports 2 common sort specs:

Given the example for the **color** facet:

Order by value in ascending order:

```
   faceSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
```

  * blue (2)
  * green (19)
  * red (21)
  * yellow (20)

Order by hit count in descending order:

```
   faceSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
```

  * red (21)
  * yellow (20)
  * green (19)
  * blue (2)

### Custom Facet Sort spec ###

It is also possible to extend the api by providing your own Facet sort spec:

First, implement the **ComparatorFactory** interface:

```
public interface ComparatorFactory{
   Comparator<Integer> newComparator(FieldValueAccessor fieldValueAccessor,int[] counts);
   Comparator<BrowseFacet> newComparator();
}
```

As an example, sort by hit count in descending order is implemented using custom comparator factory:

```
public class FacetHitcountComparatorFactory implements ComparatorFactory {
	public Comparator<Integer> newComparator(FieldValueAccessor valueList,
			final int[] counts) {
		return new Comparator<Integer>(){

			public int compare(Integer f1, Integer f2) {
				int val = counts[f1] - counts[f2];
				if (val==0)
		        {
		            val=f2-f1;
		        }
		        return val;
			}
			
		};
	}
	
	public Comparator<BrowseFacet> newComparator() {
		return new Comparator<BrowseFacet>(){

		        public int compare(BrowseFacet f1, BrowseFacet f2) {
			      int val = f1.getHitCount() - f2.getHitCount();
			      if (val==0)
	                      {
	                        val=f1.getValue().compareTo(f2.getValue());
	                      }
	                      return val;
		        }		
	        };
	}
}
```

Second, set the instance on the FacetSpec and also, the the order by enum to OrderByCustom:

```
   facetSpec.setOrderBy(FacetSortSpec.OrderByCustom);
   facetSpec.setCustomComparatorFactory(myComparatorFactory);
```

Please note that if custom comparator factory is not set, yet the order by enum is set to OrderByCustom, an **IllegalArgumentException** will be thrown during browse.