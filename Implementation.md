# Implementation: #

Bobo Browse is an extension to the Lucene full-text search engine that provides fast browsing and field retrieval features.

## FacetHandlers: ##

To make a field in a Lucene index support facet, a corresponding FacetHandler object needs to be defined and added to BoboIndexReader, which derives from Lucenes' IndexReader (http://lucene.apache.org/java/2_4_1/api/core/org/apache/lucene/index/IndexReader.html) class.

The access pattern for a FacetHandler is that it is a Builder for Lucene Filters as well as a factory that creates a doc collector, e.g. FacetCountCollector that accumulates count information for the facet values. The efficient implementation of FacetHandlers is essential to performance of Bobo Browse. By default, a set of predefined FacetHandlers is provided that should support most data types. Please refer to the [FacetHandler wiki](FacetHandler.md) for detailed information on how the default FacetHandlers are implemented and how to write a custome FacetHandler.

Examples for configuration of FacetHandlers can be found [here](CreateIndex.md).

### Runtime FacetHandlers: ###

Sometimes data is not available a searcher load time, the Bobo Browse api supports the ability to set runtime FacetHandlers. In the case of name collision, FacetHandlers set at runtime will be used instead.

## Browse (Query, Filter and Count): ##

While lucene already has support for Filters in addtion to Queries, Browse Engine extends the model by also provide the ability to count the hits for a facet. This is more complex than just implementing the HitCollector interface, because facet counting should be done there are situations where the requirements of Browse Engine dictates when the Filtering happens, and counting is not neccessarily on only the match docs. (We will get in more details in the following Expand Selection section)

### Expand Selection: ###

This is a feature of Bobo Browse that is unique such that, sometimes it is useful for each facet to show alternative selections in the same field when a selection has already been made. E.g. in the cardemo, selecting "color=Red", although the matched docs would filter out facets for other colors, they are shown regardless. However, if another selection is made on a different field, e.g. category=SUV, all the color facets will have the intersection applied.

To enable this feature, call !setExpandSelection on the FacetSpec class.