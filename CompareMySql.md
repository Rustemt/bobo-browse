# Introduction #

We did some performance comparison with similar functionality in MySql.

# Setup #

Data source: car data, [http://bobo-browse.googlecode.com/svn/trunk/cardata/data/dataout.txt](http://bobo-browse.googlecode.com/svn/trunk/cardata/data/dataout.txt)

We replicated car data 200 times to get to 3M docs, and create a Lucene index with 20 segments. This is a reasonable index structure during realtime scenarios, and would serve as a loose upper-bound for query latency.

We also created a MySql table:

(using:
```
CREATE TABLE IF NOT EXISTS cars(id INT NOT NULL AUTO_INCREMENT,PRIMARY KEY(id),color VARCHAR(15),
category VARCHAR(30),year INT,price FLOAT,mileage INT,tags VARCHAR(255),makemodel VARCHAR(255),
city VARCHAR(255),body VARCHAR(255),fulltext(body));
```
)

```
mysql> desc cars;
+-----------+--------------+------+-----+---------+----------------+
| Field     | Type         | Null | Key | Default | Extra          |
+-----------+--------------+------+-----+---------+----------------+
| id        | int(11)      | NO   | PRI | NULL    | auto_increment |
| color     | varchar(15)  | YES  | MUL | NULL    |                |
| category  | varchar(30)  | YES  |     | NULL    |                |
| year      | int(11)      | YES  |     | NULL    |                |
| price     | float        | YES  | MUL | NULL    |                |
| mileage   | int(11)      | YES  |     | NULL    |                |
| tags      | varchar(255) | YES  |     | NULL    |                |
| makemodel | varchar(255) | YES  |     | NULL    |                |
| city      | varchar(255) | YES  |     | NULL    |                |
| body      | varchar(255) | YES  | MUL | NULL    |                |
+-----------+--------------+------+-----+---------+----------------+
10 rows in set (0.00 sec)
```

And inserted the same 3M cars.

**Note:** columns **color** and **price** have indexes, and **body** is a fulltext field containing tokens from: color,category,tags, makemodel, and city.

Query latency:

  * Simple select on mileage, a range query, including result count. Bobo automatically returns the count, which translate to the following mysql queries:
```
mysql> select id from cars where mileage>'12750' limit 1,2;
+--------+
| id     |
+--------+
| 381572 |
| 381573 |
+--------+
2 rows in set (0.00 sec)

mysql> select count(*) from cars where mileage>'12750';
+----------+
| count(*) |
+----------+
|  1803200 |
+----------+
1 row in set (2.09 sec)

```

> Bobo: 81ms


  * Sorting on one field with no index:
```
      mysql> select id from cars where mileage>'12750' order by category limit 1,10;
+--------+
| id     |
+--------+
| 400461 |
| 395654 |
| 395655 |
| 395656 |
| 395657 |
| 395658 |
| 395659 |
| 395660 |
| 395661 |
| 395662 |
+--------+
10 rows in set (3.87 sec)
```

Bobo: 112ms


  * Sorting on one indexed field:

```
mysql> select id from cars where mileage>'12750' order by color limit 1,10;
+--------+
| id     |
+--------+
| 381625 |
| 381627 |
| 381630 |
| 381633 |
| 381635 |
| 381636 |
| 381640 |
| 381651 |
| 381654 |
| 381660 |
+--------+
10 rows in set (0.00 sec)
```

Bobo: 118ms

  * Sorting on two indexed fields:

```
mysql> select id from cars where mileage>'12750' order by color,price limit 1,10;
+--------+
| id     |
+--------+
| 413775 |
| 383775 |
| 428775 |
| 443775 |
| 503775 |
| 488775 |
| 518775 |
| 548775 |
| 533775 |
| 458775 |
+--------+
10 rows in set (3.79 sec)
```

Bobo: 122ms

  * Sorting on three fields, 2 index, 1 not indexed:
```
mysql> select id from cars where mileage>'12750' order by color,price,category limit 1,10;
+--------+
| id     |
+--------+
| 398775 |
| 428775 |
| 443775 |
| 413775 |
| 458775 |
| 473775 |
| 488775 |
| 518775 |
| 533775 |
| 503775 |
+--------+
10 rows in set (4.20 sec)
```

Bobo: 123ms

  * Group By on a field with an index:
```
mysql> select color,count(*) from cars where mileage>'12750' group by color limit 1,10;
+--------+----------+
| color  | count(*) |
+--------+----------+
| blue   |   135000 |
| gold   |   131400 |
| green  |   128400 |
| red    |   254200 |
| silver |   249200 |
| white  |   265200 |
| yellow |   264800 |
+--------+----------+
7 rows in set (24.01 sec)
```

Bobo: 119ms

  * Group By on a field without an index:
```
mysql> select category,count(*) from cars where mileage>'12750' group by category limit 1,10;
+---------------+----------+
| category      | count(*) |
+---------------+----------+
| exotic        |   222600 |
| luxury        |   330600 |
| mini-van      |   105200 |
| sedan         |   133200 |
| sports car    |   193400 |
| station wagon |    27800 |
| sub-compact   |    58800 |
| suv           |   163800 |
| truck         |    56800 |
| van           |    19600 |
+---------------+----------+
10 rows in set (7.04 sec)
```

Bobo: 120ms

  * With text search:
```
mysql> select id from cars where mileage>'12750' AND match(body) against('cool') limit 1,10;
+---------+
| id      |
+---------+
| 2434885 |
|  406778 |
| 2348257 |
| 3304885 |
| 1080126 |
| 1080130 |
| 1616515 |
| 3071515 |
| 2190126 |
|  752297 |
+---------+
10 rows in set (1.49 sec)
```

Bobo: 128ms

  * With text search, with sort
```
mysql> select id from cars where mileage>'12750' AND match(body) against('cool') order by category limit 1,10;
+--------+
| id     |
+--------+
| 381577 |
| 381578 |
| 381668 |
| 381669 |
| 381670 |
| 381671 |
| 381672 |
| 381673 |
| 381675 |
| 381676 |
+--------+
10 rows in set (12.83 sec)
```
Bobo: 158ms

  * With text search, with sort and group by
```
mysql> select color,count(*) from cars where mileage>'12750' AND match(body) against('cool') group by color order by category limit 1,10;
+--------+----------+
| color  | count(*) |
+--------+----------+
| gold   |    63600 |
| silver |   119600 |
| green  |    64200 |
| blue   |    69000 |
| black  |   185600 |
| red    |   123800 |
| yellow |   135800 |
+--------+----------+
7 rows in set (13.88 sec)
```

Bobo: 172ms