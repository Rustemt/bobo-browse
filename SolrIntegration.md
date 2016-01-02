# Solr Integration #

## Introduction ##

Some main differences between Bobo-Browse and Solr Facet Search:
  * Bobo is a library, just like Lucene, where Solr Facet is built into the Solr container.
  * Bobo is designed to be extensive through a plugin framework to allow for different types of facet support. Out of the box, Bobo supports simple,multi,range and hierarchical facet support.
  * Bobo does not depend on request caching for performance. e.g. "color=red" does not need to be warmed up, in a way, all values and combinations are cached.
  * ...

Solr container gives a out-of-box experience for Lucene through a clean and simple application. So it makes a lot of sense to be extend Solr with Bobo.

With the custom IndexReaderFactory support ([SOLR-243](https://issues.apache.org/jira/browse/SOLR-243), target release 1.4) We have built a plugin to run bobo inside the Solr container.

## Configuration ##

_solrconfig.xml_ neends to be modified to hook up the plugin:

### IndexReaderFactory ###

Install BoboIndexReaderFactory as a custom IndexReaderFactory

```
<indexReaderFactory name="IndexReaderFactory" class="com.browseengine.solr.BoboIndexReaderFactory">
</indexReaderFactory >
```

### RequestHandler ###

Install BoboRequestHandler as a custom SolrRequestHandler with the name: bobo

```
<requestHandler name="bobo" class="com.browseengine.solr.BoboRequestHandler" >
</requestHandler>
```

See also: [SolrRequestHandler on Solr Wiki](http://wiki.apache.org/solr/SolrRequestHandler)

### ResponseWriter ###

Install !BoboXMLResponseWriter as a custom QueryResponseWriter with the name: bobo

```
<queryResponseWriter name="bobo" class="com.browseengine.solr.BoboXMLResponseWriter">
</queryResponseWriter>
```

See also: [QueryResponseWriter on Solr Wiki](http://wiki.apache.org/solr/QueryResponseWriter)

## Jars ##

Currently there is a problem with conflicting ClassLoader between Solr container and Spring. To work around it, we need to bundle the following jars with **solr.war**, e.g. in the WEB-INF/lib directory:

| **jar name** | **location** | **comments** | **url** |
|:-------------|:-------------|:-------------|:--------|
|kamikaze-1.0.7.jar|lib           |lucene enhancements|http://code.google.com/p/lucene-ext|
|fastutil-5.1.5.jar|lib           |fast primitive collections api|http://fastutil.dsi.unimi.it/|
|spring-2.5.5.jar|lib           |spring dependency injection framework|http://www.springsource.org/|
|xstream-1.2.jar|lib           |xml serialization library|http://xstream.codehaus.org/|
|bobo-browse.jar|dist          |bobo library, build via ant|         |
|bobo-solr.jar |dist          |bobo solr plugin, build via ant|         |

## Syntax ##

After solrconfig.xml is edited, and jars installed, starting Solr should be ready support bobo queries.

The following is a reference on the bobo request syntax:

| **param name** | **comments**| **example** |
|:---------------|:------------|:------------|
| qt             | specifies bobo request handler, value would be the name we gave in the solrconfig.xml | qt=bobo     |
| wt             | specifies bobo xml response writer, value would be the name we gave in the solrconfig.xml | wt=bobo     |
| start `**`     | specifies paging parameter: offset | start=1     |
| rows `**`      | specifies paging parameter: count | rows=10     |
| sort           | specifies hits sorting parameter | sort=price asc,year desc |
| q `**`         | specifies query string, if not specified, all non-deleted docs will be considered a hit | q=cool      |
| df `**`        | specifies default field | df=contents |
| bobo.sel.${name}.val | specifies a selection value, where ${name} is the name of a defined facet handler`*` | bobo.sel.color.val=red |
| bobo.sel.${name}.notval | specifies a selection not value, where ${name} is the name of a defined facet handler`*` | bobo.sel.color.notval=green |
| bobo.sel.${name}.operation | specifies multiple settings of values are to be AND or OR, where ${name} is the name of a defined facet handler | bobo.sel.color.operation=and |
| bobo.sel.${name}.depth | specifies depth for PathFacetHandler types, where ${name} is the name of a defined facet handler | bobo.sel.city.depth=1 |
| bobo.sel.${name}.strict | specifies strict value for PathFacetHandler types, where ${name} is the name of a defined facet handler | bobo.sel.city.strict=false |
| bobo.groupby.${name} | specifies facet spec, where ${name} is the name of a defined facet handler | bobo.groupby.color |
| bobo.groupby.${name}.expand | specifies expand property of the facet spec, where ${name} is the name of a defined facet handler | bobo.groupby.color.expand=true |
| bobo.groupby.${name}.max | specifies max property of the facet spec, where ${name} is the name of a defined facet handler | bobo.groupby.color.max=10 |
| bobo.groupby.${name}.mincount | specifies !minHitCount property of the facet spec, where ${name} is the name of a defined facet handler | bobo.groupby.color.mincount=1 |
| bobo.groupby.${name}.orderby | specifies sorting property of the facet spec, e.g. by hits or by value, where ${name} is the name of a defined facet handler | bobo.groupby.color.orderby=hits |

`*` multiple values maybe added by specifying the same parameter multiple times with different values.

`**` These values are defined by Solr and should follow Solr semantics.

The following is the request string for:

cagtegory=suv AND
color=green
GROUPING ON
category,color,price

```
http://localhost:8983/solr/select?wt=bobo&qt=bobo&bobo.sel.category.val=suv&bobo.sel.color.val=green&bobo.groupby.color&bobo.groupby.category&bobo.groupby.tags
```

Screenshot of the results:
![http://john.wang.googlepages.com/bobo-solr.png](http://john.wang.googlepages.com/bobo-solr.png)

## Shards ##

Bobo-Solr request handler also supports sharding using the same request format as Solr, e.g.:

```
http://localhost:8983/solr/select?wt=bobo&qt=bobo&bobo.sel.category.val=suv&bobo.sel.color.val=green&bobo.groupby.color&bobo.groupby.category&bobo.groupby.tags&shards=localhost:8983/solr,jwang-md:8983/solr,jwang-md:8983/solr
```

Screenshot of the results:
![http://john.wang.googlepages.com/bobo-solr-shard.png](http://john.wang.googlepages.com/bobo-solr-shard.png)

