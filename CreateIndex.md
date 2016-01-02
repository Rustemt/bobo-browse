# Creating a Browse Index #

A Browse Index is simply a Lucene index with an additional bobo.spring file that tells browse engine about the structuring of the index data.

Spring is the default dependency injection framework and bobo.spring is simply a spring construct of a set of FacetHandler objects., e.g.

```
<bean id="color" class="com.browseengine.bobo.facets.impl.SimpleFacetHandler">
    <constructor-arg value="color" />
</bean>
```


## Prebuilt FacetHandler types: ##

**simple:** (`com.browseengine.bobo.facets.impl.SimpleFacetHandler`)
Used when there is a discrete set of facet values, for example: color, with values: red,green,blue,white,black.
Each document can have only 1 value in this field. When being indexed, this field should not be tokenized.

**multi:** (`com.browseengine.bobo.facets.impl.MultiValueFacetHandler`)
Similar to simple type field, multi field allows a document to have multiple values. When being indexed, this field can be tokenized. Or alternatively, one can index multiple values in multiple document fields under the same field name.

**compact multi:** (`com.browseengine.bobo.facets.impl.CompactMultiValueFacetHandler`)
Same as MultiValueFacetHandler, multiple values are allowed, the total possible values are limited to 32. However, this is more efficient than MultiValueFacetHandler and has a smaller memory footprint.

**path:** (`com.browseengine.bobo.facets.impl.PathFacetHandler`)
Used to denote facet values with hierarchical structure, for example: "A/B/C/D"
Each document can have only 1 value in this field. When being indexed, this field should not be tokenized.

**range:** (`com.browseengine.bobo.facets.impl.RangeFacetHandler`)
Used to denote a range of facet, e.g. dates, prices etc.
Each document can have only 1 value in this field. When being indexed, this field should not be tokenized. Furthermore, the values need to be formatted to ensure sorting by lexical order is the same as the value order.

## TermValueList ##

TermValueList is an immutable implementation of `List<String>` with some additional contracts:

  * seal() trims the unused elements
  * indexOf() return value has the same semantic as Arrays.binarySearch()


Sometimes given the nature of the field values, it is more efficient to build implementations of TermValueList to contain primitive types.

## TermListFactory ##

This is a factory class that builds an instance of TermValueList. When not specified, TermStringList instances are created.


## Example: ##
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
 
    <bean id="color" class="com.browseengine.bobo.facets.impl.SimpleFacetHandler">
        <constructor-arg value="color" />
    </bean>
    
    <bean id="category" class="com.browseengine.bobo.facets.impl.SimpleFacetHandler">
        <constructor-arg value="category" />
    </bean>
    
    <bean id="city" class="com.browseengine.bobo.facets.impl.PathFacetHandler">
        <constructor-arg value="city" />
        <property name="separator" value="/" />
    </bean>
    
    <bean id="makemodel" class="com.browseengine.bobo.facets.impl.PathFacetHandler">
        <constructor-arg value="makemodel" />
        <property name="separator" value="/" />
    </bean>
    
    <bean id="year" class="com.browseengine.bobo.facets.impl.RangeFacetHandler">
        <constructor-arg value="year" />
        <constructor-arg value="true" />
    </bean>
    
    <bean id="price" class="com.browseengine.bobo.facets.impl.RangeFacetHandler">
        <constructor-arg value="price" />
        <constructor-arg>
            <bean class="com.browseengine.bobo.facets.data.PredefinedTermListFactory">
                <constructor-arg value="java.lang.Float" />
                <constructor-arg value="00000000000000000000" />
            </bean>
        </constructor-arg>
        <constructor-arg value="true" />
    </bean>
    
    <bean id="mileage" class="com.browseengine.bobo.facets.impl.RangeFacetHandler">
        <constructor-arg value="mileage" />
        <constructor-arg>
            <bean class="com.browseengine.bobo.facets.data.PredefinedTermListFactory">
                <constructor-arg value="java.lang.Integer"/>
                <constructor-arg value="00000000000000000000" />
            </bean>
        </constructor-arg>
        <constructor-arg>
             <list>
                <value>[* TO 12500]</value>
                <value>[125001 TO 15000]</value>
                <value>[15001 TO 17500]</value>
                <value>[17501 TO *]</value>
             </list>
        </constructor-arg>
    </bean>
 
    <bean id="tags" class="com.browseengine.bobo.facets.impl.MultiValueFacetHandler">
        <constructor-arg value="tags" />
        <constructor-arg value="15" />
    </bean>
    
    <bean id="handlers" class="java.util.ArrayList">
        <constructor-arg>
            <list>
                <ref bean="color" />
                <ref bean="category" />
                <ref bean="city" />
                <ref bean="makemodel" />
                <ref bean="year" />
                <ref bean="price" />
                <ref bean="mileage" />
                <ref bean="tags" />
            </list>
        </constructor-arg>
    </bean>
</beans>
```