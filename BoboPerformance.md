# Introduction #

This page describes how to run a simple performance benchmarking tool to measure latency and through-put on the Bobo library.

# Getting Started #

The tool is ready to run out of the box, simply do:

**ant perf**

and a performance summary will be printed to console. By default, this measures 100 requests against the default cardemo data (15k documents)

# Diagram #
![http://john.wang.googlepages.com/bobo-perf.png](http://john.wang.googlepages.com/bobo-perf.png)

# Configuration #

For configuration, edit the_**bobo-perf/conf/bobo-perf.properties**_ file:

| **parameter** | **required** | **example** | **description** |
|:--------------|:-------------|:------------|:----------------|
| num.threads   | no, default = 10) | 10          | number of threads |
| throttle.wait | no, dfeault = 500) | 500         | time in milliseconds each thread will wait before executing another requests |
| index.dir     | yes          | ../../cardata/cartag | index directory |
| num.req       | yes          | 100         | number of requests to execute for the session `*` |
| query.log.file | yes          | req.log     | file containing requests per line `**` |

`*` if the number of requests in the query.log.file is less the this specified number, the tool will replicate requests up to specified number

`**` requests are the **BrowseRequest** objects text formatted by [Google Protobuf](http://code.google.com/p/protobuf/).

```
  BrowseRequest br = ...;
  String reqString = BrowseProtobufConverter.toProtoBufString(br);
```