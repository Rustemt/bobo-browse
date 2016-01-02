# Getting Started: #

## Prepare a browse index: ##

A browse index is a Lucene index with field description information. Bobo Browse does not currently provide any extensive indexing wrappers on top of Lucene, so indexing with the standard Lucene API (http://lucene.apache.org/java/2_4_1/api/core/index.html) would suffice.

See [Creating a Browse Index](CreateIndex.md) for details.

Search and Browse:

The Bobo Browse Engine is released as a library, the API is published here.
Familiarity with the Lucene API is helpful.

Here are some of the basic concepts or objects to understand:

  * BrowseSelection: A selection or filter to be applied, e.g. Color=Red
  * FacetSpec: Specifies how facets are to be returned on the result object, e.g. Top 10 facets of car types ordered by count with a min count of 5
  * BrowseRequest: A set of BrowseSelections, a keyword text query, and a set of FacetSpecs.
  * BrowseFacet: a facet, (a string value with a hit count)
  * FacetCollection: A Collection object of BrowseFacets
  * BrowseResult: Result of a browse operation.
  * FacetHandler: A plugin into the browse engine to knows how to manipulate facet data.
  * BoboIndexReader: A Lucene IndexReader containing a List of FacetHandlers.


## Example: ##

```
// define facet handlers
	  
	    // color facet handler
	    SimpleFacetHandler colorHandler = new SimpleFacetHandler("color");
	    
	    // category facet handler
	    SimpleFacetHandler categoryHandler = new SimpleFacetHandler("category");
	    
	    List<FacetHandler> handlerList = Arrays.asList(new FacetHandler[]{colorHandler,categoryHandler});
	    
		// opening a lucene index
		  Directory idx = FSDirectory.open(new File("myidx"));
		  IndexReader reader = IndexReader.open(idx,true);
		   
		  // decorate it with a bobo index reader
		  BoboIndexReader boboReader = BoboIndexReader.getInstance(reader,handlerList);
		   
		  // creating a browse request
		  BrowseRequest br=new BrowseRequest();
		  br.setCount(10);
		  br.setOffset(0);
		   
		  // add a selection
		  BrowseSelection sel=new BrowseSelection("color");
		  sel.addValue("red");
		  br.addSelection(sel);
		   
		  // parse a query
		  QueryParser parser = new QueryParser("contents",new StandardAnalyzer(Version.LUCENE_CURRENT));
		  Query q=parser.parse("cool car");
		  br.setQuery(q);
		   
		  // add the facet output specs
		  FacetSpec colorSpec = new FacetSpec();
		  colorSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		   
		  FacetSpec categorySpec = new FacetSpec();
		  categorySpec.setMinHitCount(2);
		  categorySpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		   
		  br.setFacetSpec("color",colorSpec);
		  br.setFacetSpec("category",categorySpec);
		   
		  // perform browse
		  Browsable browser=new BoboBrowser(boboReader);
		  BrowseResult result=browser.browse(br);
		   
		  int totalHits = result.getNumHits();
		  BrowseHit[] hits = result.getHits();
		   
		  Map<String,FacetAccessible> facetMap = result.getFacetMap();
		   
		  FacetAccessible colorFacets = facetMap.get("color");
		  List<BrowseFacet> facetVals = colorFacets.getFacets();
```