# Introduction #

When building custom facet handlers, it is useful to build on existing facet handlers. The Bobo Browse api provides a framework for building composite facet handlers.

### Specifying facet handlers to build from ###

On the FacetHandler class constructor, you can pass in a Set of facet handler names that you wish to access within your facet handler through the **dependsOn** parameter.

BoboIndexReader will guarantee to load the dependent facet handlers before your custom facet handler. (the dependency graphy will be checked, and an IOException will be thrown if circular dependencies are detected)

Dependent facet handler instances can be accessed through the BoboIndexReader instance (typically from the load() method):

```
@Override
public void load(BoboIndexReader reader) throws IOException {
	FacetHandler handler1 = reader.getFacetHandler("usefulHandler1");
	FacetHandler handler2 = reader.getFacetHandler("usefulHandler2");
}
```

# Examples #

## SimpleGroubyFacetHandler ##

This is a composite facet handler that depend on a set of SimpleFacetHandler instances and provides a SQL-GroupBy-like functionality/view on the resulting facets values, e.g.

```
SELECT count(*) FROM ... GROUP BY color,shape,number;
```

Specify the instance:

```
LinkedHashSet<String> dependsNames=new LinkedHashSet<String>();
dependsNames.add("color");
dependsNames.add("shape");
dependsNames.add("number");
SimpleGroubyFacetHandler handler = new SimpleGroupbyFacetHandler("groupby", dependsNames,",");   // separator is the comma
```

Build the selection:

```
BrowseSelection sel=new BrowseSelection("groupby");
sel.addValue("red,square");
req.addSelection(sel);

FacetSpec fspec = new FacetSpec();
req.setFacetSpec("groupby", fspec);

```

Output:

```
facets=[red,square,0005(1), red,square,0010(1)]
```

## FilteredRangeFacetHandler ##

This is a facet handler to builds on another range facet handler with a different set of pre-defined ranges.

Specify the instance:

```
String[] ranges = new String[]{"[2001/01/01 TO 2001/12/30]","[2007/01/01 TO 2007/12/30]"};
FilteredRangeFacetHandler handler = new FilteredRangeFacetHandler("filtered_date", "date",Arrays.asList(ranges));
```

Build the selection:

```
req.setFacetSpec("filtered_date", new FacetSpec());		
```

Output:

```
facets=[[2001/01/01 TO 2001/12/30](1), [2007/01/01 TO 2007/12/30](1)]
```

Note that this facet handle can be instantiated at runtime and added as a runtime-facet handler:

```
BoboBrowser browser = new BoboBrowser();
browser.setFacetHandler(handler);
```

This is especially useful when we don't know the range set until query time, e.g. "2 days ago from now".