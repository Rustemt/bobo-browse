# Bobo issue tracking has been moved to: http://linkedin.jira.com/browse/BOBO #
# Bobo wiki has been moved to: http://linkedin.jira.com/wiki/display/BOBO/Home #
# Bobo source control has been moved to: http://github.com/javasoze/bobo #

## Bobo-Browse ##

is a [Faceted Search](http://en.wikipedia.org/wiki/Faceted_search) implementation written purely in Java, an extension of [Apache Lucene](http://lucene.apache.org).

While Lucene is good with unstructured data, Bobo fills in the missing piece to handle semi-structured and structured data.

Bobo Browse is an information retrieval technology that provides navigational browsing into a semi-structured dataset. Beyond the result set from queries and selections, Bobo Browse also provides the facets from this point of browsing.


## Logical Architecture ##
![http://john.wang.googlepages.com/bobo-arch.png](http://john.wang.googlepages.com/bobo-arch.png)


Some of the extra features:

  * No need for cache warm-up for the system to perform.
  * multi value sort - sort documents on fields that have multiple values per doc, .e.g tokenized fields
  * fast field value retrieval - over 30x faster than ` IndexReader.document(int docid) `
  * facet count distribution analysis
  * stable and small memory footprint
  * support for runtime faceting
  * result merge library for distributed facet search
  * ...


**Release:** - Bobo Browse 2.5.0-rc1 is released! (2/15/2010)

**News:** - Bobo is powering the new LinkedIn Faceted People Search (http://www.linkedin.com)

See TechCrunch article at:
(http://www.techcrunch.com/2009/07/15/linkedin-drills-down-into-people-search-with-new-beta/)



---

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products: [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and [YourKit ASP.NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
