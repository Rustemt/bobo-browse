# Introduction #

**currently only available on the BR\_DEV\_2\_1\_0 branch**

For the following type of boolean queries, we can leverage facet handlers' data structure to build highly performing Query objects with enhanced scoring flexibilities:

  * f1:v1<sup>b1</sup> OR f1:v2<sup>b2</sup> OR f1:v3<sup>b3</sup> OR ...
  * f1:v1<sup>b1</sup> AND f1:v2<sup>b2</sup> AND f1:v3<sup>b3</sup> AND ...

The characteristics of the above types of queries are:

  * values in all clauses are in the same field
  * operators of all clauses are of the same type

# Implementation #

Let's first look at how you would this using the standard Lucene API:

```
  // replace Occur.SHOULD with Occur.MUST to produce conjunctive clauses
   BooleanQuery resultQ = new BooleanQuery();

   
   TermQuery tq = new TermQuery(new Term(f1,v1));
   tq.setBoost(b1);
   resultQ.addClause(tq,Occur.SHOULD);

  tq = new TermQuery(new Term(f1,v2));
   tq.setBoost(b2);
   resultQ.addClause(tq,Occur.SHOULD);

   tq = new TermQuery(new Term(f1,v3));
   tq.setBoost(b3);
   resultQ.addClause(tq,Occur.SHOULD);

   ...
```

And this is what you can do to build exactly the same query with Bobo FacetHandlers:

```

  // assumption: we have a FacetHandler of name f1, and it implements FacetScorable interface
  BrowseSelection sel = new BrowseSelection(f1);
  sel.setValues(new String[]{v1,v2,v3,...});
  
  Map<String,Float> boostMap = new HashMap<String,Float>();
  boostMap.put(v1,b1);
  boostMap.put(v2,b2);
  boostMap.put(v3,b3);
  
  ...

  // to produce conjunctive clauses:
  // sel.setSelectionOperation(ValueOperation.ValueOperationAnd);  // default is OR

  FacetTermQuery resultQ = new FacetTermQuery(sel,boostMap);
```

The two above snippets accomplishes the same thing.

# Performance Comparison #

  * Dataset: expanded the car demo dataset to 3 million cars.
  * Java version: 1.6.0\_13
  * JVM options: -Xms1g -Xmx1g -server
  * HW: iMac, 2.4 GHZ INTEL CORE 2 Duo, 4GB 667 MHz DDR2 SDRAM

Benchmark two types of field data:

  1. single valued - each doc has at most 1 value in the field (SimpleFacetHandler)
  1. multi valued - each doc has any number of values in the field (MultiValueFacetHandler)

Single Valued:

| **number values/clauses** | **Bobo FacetTermQuery  (OR)** | **Lucene  BooleanQuery (OR)** |  **Bobo FacetTermQuery  (AND)** | **Lucene  BooleanQuery (AND)** |
|:--------------------------|:------------------------------|:------------------------------|:--------------------------------|:-------------------------------|
| 2                         | 57 ms                         | 95 ms                         | 0 ms                            | 14 ms                          |
| 5                         | 93 ms                         | 170 ms                        | 0 ms                            | 20 ms                          |
| 10                        | 118 ms                        | 230 ms                        | 0 ms                            | 20 ms                          |

Multi Valued:

| **number values/clauses** | **Bobo FacetTermQuery (OR)** | **Lucene BooleanQuery (OR)** |  **Bobo FacetTermQuery  (AND)** | **Lucene  BooleanQuery (AND)** |
|:--------------------------|:-----------------------------|:-----------------------------|:--------------------------------|:-------------------------------|
| 2                         | 165 ms                       | 210 ms                       | 107 ms                          | 114 ms                         |
| 5                         | 205 ms                       | 400 ms                       | 90 ms                           | 145 ms                         |
| 10                        | 185 ms                       | 825 ms                       | 90 ms                           | 108 ms                         |

Some explanation:

  * One may be confused why the time went down when we increased number of clause from 5 to 10 for Multi Valued data in the disjunction case. The reason here being although scoring cost increased, the iteration cost over the docid set actually went down because coverage of the clauses makes evaluation of a hit cheaper.
  * In the conjunction case for both Bobo and Lucene, as number of clauses increases, times decreased. That is because as number of clauses increase, the less hits are to be collected.

# Scoring #

With the Lucene Similarity class, scoring can be customized by modifying certain scoring parameters, e.g. tf, norm, idf etc. However, how they are composed together cannot be easily modified.

With Bobo's FacetTermQuery class, a FacetTermScoringFunctionFactory may be specified to hook in custom scoring function.

e.g.:
```
FacetTermQuery query = new FacetTermQuery(mySel,boostMap,myFacetTermScoringFunctionFactory);
```

FacetTermScoringFunctionFactory simply is a factory that creates FacetTermScoringFunction instances, which have the following APIs:

  * ` float score(int df, float boost) ` - returns a score based on document frequency of the term and a boost
  * ` void clearScores() ` - when there are multiple terms to be scored, the api follows a collector pattern, and this method initializes the collection
  * ` void scoreAndCollect() ` - when there are multiple terms to be score, this api calculates the score and collects it internally, !getCurrentScore() will be called to fetch a combined score from the collected scores
  * ` float getCurrentScore() ` - gets a combined score from the collected scores

Code example:

```

// this is actually the default scoring function if no FacetTermScoringFunction is specified.

public class DefaultFacetTermScoringFunction implements FacetTermScoringFunction {
	private float _sum=0.0f;
	
	public final void clearScores(){
		_sum = 0.0f;
	}
	
	public final float score(int df, float boost) {
		return boost;
	}
	
	public final void scoreAndCollect(int df,float boost){
		_sum+=boost;
	}

	public final float getCurrentScore() {
		return _sum;
	}

	...
}
```

There are additional contracts to provide Explanation instances for the scoring function.