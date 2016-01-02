# Release Notes #

## 2.0.8 ##
  * added ability to fetch raw field data on top of string formated field data
  * [Issue 23](https://code.google.com/p/bobo-browse/issues/detail?id=23) (John Wang)
  * [Issue 27](https://code.google.com/p/bobo-browse/issues/detail?id=27) (John Wang)
  * [Issue 29](https://code.google.com/p/bobo-browse/issues/detail?id=29) (John Wang)

## 2.0.7 ##
  * bug fix: [Issue 28](https://code.google.com/p/bobo-browse/issues/detail?id=28) (Thanks tallpsmith)
  * [Issue 19](https://code.google.com/p/bobo-browse/issues/detail?id=19): fixed bug with facet result formatting (John Wang)
  * memory enhancements (John Wang)
  * some performance enhancements (Yasuhiro)


## 2.0.6 ##
  * bug fix with default impl of FacetAccessible.getFacet with value formatting
  * added dynamic and filter range facet handlers
  * added simple grouby facet handler
  * added custom facet sort api
  * refactored/rewired cardemo
  * added realtime facet demo support using car data with zoie integration
  * started cmdline client for bobo

## 2.0.5 ##
  * bug fix for custom lucene sort

## 2.0.4 ##
Performance release

  * numerous performance enhancements
  * revived bobo solr plugin
  * added local perf benchmarking tool

## 2.0.3 ##
Bug fix release

  * Added sorting support for MultiValueFacetHandler and CompactMultiValueFacetHandler, now you can sort multi valued fields thru bobo. (John)
  * Counting performance enhancement for CompactMultiValueFacetHandler (Yasuhiro)
  * ListMerger fixes and cleanup for result merging. (Yasuhiro)

## 2.0.2 ##
Bug fix release

  * Fixed an issue where ArrayIndexOutOfBoundsException is thrown when a term exists with no docs. This can happen to the index between merges.