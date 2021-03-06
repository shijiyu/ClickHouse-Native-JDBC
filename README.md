# ClickHouse-Native-JDBC 

This is a native JDBC library for accessing [ClickHouse](https://clickhouse.yandex/) in Java.

## Features

* Uses native ClickHouse tcp client-server protocol, with higher performance than HTTP
* Compatibility with `java.sql`
* Data Compression

## Supported queries
* [x] SELECT
* [x] INSERT
* [x] CREATE
* [x] ALTER
* [x] DROP
* [x] RENAME

## Supported data types

* [x] UInt8, UInt16, UInt32, UInt64, Int8, Int16, Int32, Int64
* [x] Float32, Float64
* [x] String
* [x] FixedString(N)
* [x] Date 
* [x] DateTime
* [x] Nullable(T)
* [x] Tuple
* [x] Nested
* [x] Array(T)
* [x] Enum
* [x] UUID


## Example

Select query, see also [SimpleQuery.java](./src/main/java/examples/SimpleQuery.java)
```java
  public static void main(String[] args) throws Exception {
    Class.forName("org.houseflys.jdbc.ClickHouseDriver");
    Connection connection = DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000");

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT (number % 3 + 1) as n, sum(number) FROM numbers(10000000) GROUP BY n");

    while (rs.next()) {
      System.out.println(rs.getInt(1) + "\t" + rs.getLong(2));
    }
  }

```

All DDL,DML queries, see also [ExecuteQuery.java](./src/main/java/examples/ExecuteQuery.java)

```java
  public static void main(String[] args) throws Exception {
    Class.forName("org.houseflys.jdbc.ClickHouseDriver");
    Connection connection = DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000");

    Statement stmt = connection.createStatement();
    // drop table
    stmt.executeQuery("drop table if exists test_jdbc_example");
    // create table
    stmt.executeQuery("create table test_jdbc_example(day default toDate( toDateTime(timestamp) ), timestamp UInt32, name String, impressions UInt32) Engine=MergeTree(day, (timestamp, name), 8192)");
    // add column `costs`
    stmt.executeQuery("alter table test_jdbc_example add column costs Float32");
    // drop the table
    stmt.executeQuery("drop table test_jdbc_example");
  }
```

## TODO
* Maven central
