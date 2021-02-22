# Cassandra: Get Read, Write and Range latency, GC Pauses, Slow query and Error Rate


## Problem Statement --
    Given a list of nodes in a cluster, go and poll the metrics via JMX from each node. Find out the following: 
    1) Identify slow running queries (queries that take more than 1 sec to execute) and GC pauses for more than 5 sec, and append them in a file every 15 min. 
    2) Record error rates for reads and writes per table and per node. Append them in a file every 10 min. 
    3) Record latency (averages and totals) of range, read, and write operations per second at the server level. Append them in a file every 5 min. 


## Prerequisite --
    1) Cassandra Jmx connection should be enable (configure each node to application.properties)
    2) Jmx port should be accessible from where application is deployed
    3) For checking slow query cassandra debug log  of each node should be accessible from where application is deployed
    4) Configure application.property output path and input path



## To Deploy Application --
    1) Copy the jar to server to particular directory
    2) From command line move to that particular directory where jar is copied
    3) Execute the following command on the command line:
    "$ java -jar your-app.jar"





## References --

    1) <https://www.datadoghq.com/blog/how-to-collect-cassandra-metrics/#collecting-metrics-with-jconsole>

       the recent read latency would be calculated from the deltas of those two metrics:

       readLatency = (ReadTotalLatency1−ReadTotalLatency0)/(ReadLatency1−ReadLatency0)

    2) <https://cassandra.apache.org/doc/latest/operating/metrics.html>

    3) <https://community.datastax.com/questions/4969/identify-slow-queries.html#:~:text=Slow%20query%20logging%20was%20added,slow_query_log_timeout_in_ms%3A%20500>

       to collect slow query in cassandra

    4) <https://www.baeldung.com/spring-boot-app-as-a-service> to deploy application
