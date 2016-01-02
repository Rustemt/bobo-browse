# FacetHandlers: #

Some requirements considered when building the default set of FacetHandlers:

  * Efficient memory representation of data
  * Fast lookup between a docid and the corresponding facet value.
  * Filters should produce fast DocIdSetIterators.

# Diagram #
![http://john.wang.googlepages.com/bobo-fh-arch.png](http://john.wang.googlepages.com/bobo-fh-arch.png)

Below is a detailed description for the predefined set of FacetHandlers.

Assumptions:

Index size: m docs
Number of terms: t

## SimpleFacetHandler: ##

### Data structure: ###

  * order array, `int[]` of size m, each element indexes into the term array (actually BigIntArray is used instead of int[.md](.md) to avoid requiring large chunks of consecutive heap allocation)
  * term array, TermListValue of size t
  * freq array, `int[]` of size t, stores docFreq for a given term
  * minID array, `int[]` of size t, stores minimum ID for a given term
  * maxID array, `int[]` of size t, stores maximum ID for a given term


### Filter: ###

Given a term, we do binary search to find the index of the term in the term array, say index = i (if not found, EmptyFilter is used)
We iterate from `minID[i]` to `maxID[i]`, match on `order[docid]` that equals to i.

### FacetCountCollector: ###

A count array, `int[]` of size t, is created to store the hit count for each term, given a match docid, `count[order[docid]]` is incremented. Facets are created for all elements in the term array with count >= minHitCount specified by the FacetSpec.

## RangeFacetHandler: ##

### Data structure: ###

  * order array, `int[]` of size m, each element indexes into the term array (actually BigIntArray is used instead of `int[]` to avoid requiring large chunks of consecutive heap allocation)
  * term array, TermListValue of size t
  * freq array, `int[]` of size t, stores docFreq for a given term
  * minID array, `int[]` of size t, stores minimum ID for a given term
  * maxID array, `int[]` of size t, stores maximum ID for a given term


### Filter: ###

Given a term of the format `"[x TO y]"`, we do binary search to find the index of the terms x, and y in the term array, say start and end, respectively. (we use the best matched indexes to determine start and end)
We iterate from `minID[i]` to `maxID[i]`, match on `order[docid]` that are in the range: `[start,end]`

### FacetCountCollector: ###

A count array, `int[]` of size t, is created to store the hit count for each term, given a match docid, `count[order[docid]]` is incremented. Facets are created by grouping all elements in the term array with count >= minHitCount specified by the FacetSpec, into desired range facets of the format `[x TO y]`.

## PathFacetHandler: ##

### Data structure: ###

  * order array, `int[]` of size m, each element indexes into the term array (actually BigIntArray is used instead of `int[]` to avoid requiring large chunks of consecutive heap allocation)
  * term array, TermListValue of size t
  * freq array, `int[]` of size t, stores docFreq for a given term
  * minID array, `int[]` of size t, stores minimum ID for a given term
  * maxID array, `int[]` of size t, stores maximum ID for a given term


### Filter: ###

Given a term of the format "a/b", we do binary search to find the range of terms that starts with "a/b", say start and end, respectively. (we use the best matched indexes to determine start and end)
We iterate from `minID[i]` to `maxID[i]`, match on `order[docid]` that are in the range: `[start,end]`

### FacetCountCollector: ###

A count array, `int[]` of size t, is created to store the hit count for each term, given a match docid, `count[order[docid]]` is incremented. Facets are created by folding all elements in the term array with count >= minHitCount specified by the FacetSpec, into a taxonomy.

## ComactMultiValueFacetHandler: ##

### Data structure: ###

  * order array, `int[]` of size m, each element is a bit encoding of the indexes of the matching terms the term array (actually BigIntArray is used instead of `int[]` to avoid requiring large chunks of consecutive heap allocation)
  * term array, TermListValue of size t, t must be <=32
  * freq array, `int[]` of size t, stores docFreq for a given term
  * minID array, `int[]` of size t, stores minimum ID for a given term
  * maxID array, `int[]` of size t, stores maximum ID for a given term


### Filter: ###

Given a term, we do binary search to find the index of the term in the term array, say index = i (if not found, EmptyFilter is used)
We iterate from `minID[i]` to `maxID[i]`, match on the encoding bits of `order[docid]` that ands bit i.

### FacetCountCollector: ###

A count array, `int[]` of size t, is created to store the hit count for each term, given a match docid, `count[order[docid]]` is incremented. Facets are created for all elements in the term array with count >= minHitCount specified by the FacetSpec.

## MultiValueFacetHandler: ##

### Data structure: ###

  * order array, `int[]` of size m, each element is a bit encoding of a 2 part value, start and end that index into the aux array (actually BigIntArray is used instead of `int[]` to avoid requiring large chunks of consecutive heap allocation)
  * aux array, `int[]` of size >=m, is an aggregate of multiple values indexed in term array for a given docid. (actually BigIntArray is used instead of `int[]` to avoid requiring large chunks of consecutive heap allocation)
  * term array, TermListValue of size t, t must be <=32
  * freq array, `int[]` of size t, stores docFreq for a given term
  * minID array, `int[]` of size t, stores minimum ID for a given term
  * maxID array, `int[]` of size t, stores maximum ID for a given term


### Filter: ###

Given a term, we do binary search to find the index of the term in the term array, say index = i (if not found, EmptyFilter is used)
We iterate from `minID[i]` to `maxID[i]`, from on the encoding bits of `order[docid]`, we determine the start and end value into aux array, and iterate throw the aux between start and end and match on `aux[x] = i`.

### FacetCountCollector: ###

A count array, `int[]` of size t, is created to store the hit count for each term, given a match docid, `count[order[docid]]` is incremented. Facets are created for all elements in the term array with count >= minHitCount specified by the FacetSpec.

## GeoSimpleFacetHandler: ##

### Data structure: ###

Used to filter documents based on distance metric – latitude and longitude.  This is a dynamic facet handler that builds on top of two range facet handlers on two fields – latitude and longitude.  The distance metric calculation in this facet handler is the delta of document’s latitude/longitude and the latitude/longitude values of the user’s point of interest.

### Filters: ###

Given a term of the format "<latitude, longitude, radius>", we convert it to two ranges ‘[– radius, latitude + radius](latitude.md)’ and ‘[– radius, longitude + radius](longitude.md)’ and pass these two ranges to the two underlying range facet handlers.

### FacetCountCollector: ###

A count array, int[.md](.md) of size t, is created to store the hit count for each term, given a match docid, count[order[docid](docid.md)] is incremented. Facets are created by grouping all elements in the term array with count >= minHitCount specified by the FacetSpec, into desired range facets of the format <latitude, longitude, radius>.

## GeoFacetHandler: ##

n is the number of distance facets specified in the user query

Used to filter documents based on distance metric – latitude and longitude.  This is a dynamic facet handler. The distance metric calculation in this facet handler is the dot product of document’s x-y-z and the x-y-z values of the user’s point of interest.

### Data structure: ###

  * xValArray array, int[.md](.md) of size m, each element is the x coordinate value of the docid (actually BigFloatArray is used instead of int[.md](.md) to avoid requiring large chunks of consecutive heap allocation)
  * yValArray array, int[.md](.md) of size m, each element is the y coordinate value of the docid (actually BigFloatArray is used instead of int[.md](.md) to avoid requiring large chunks of consecutive heap allocation)
  * zValArray array, int[.md](.md) of size m, each element is the z coordinate value of the docid (actually BigFloatArray is used instead of int[.md](.md) to avoid requiring large chunks of consecutive heap allocation)

### Filter: ###

Given a term of the format "<latitude, longitude, radius>", we convert it to x-y-z coordinates and cosine radius and calculate the distance metric as the dot product of the user-specified x-y-z coordinates and the x-y-z coordinates of the document.

### FacetCountCollector: ###

A count array, int[.md](.md) of size n, is created to store the hit count for each term, given a match docid, count[order[docid](docid.md)] is incremented. Facets are created by grouping all elements in the term array with count >= minHitCount specified by the FacetSpec, into desired range facets of the format <latitude, longitude, radius>.