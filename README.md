# For cassandra to get read ,write and range latency ,gc pauses ,slow query and error rate

Prerequisite --
1) Cassandra Jmx connection should be enable (configure each node to application.properties)
2) Jmx port should be accessible from where application is deployed
3) For checking slow query cassandra debug log  of each node should be accessible from where application is deployed
4) Configure application.property output path and input path



To Deploy Application --
copy the  jar to server

The executable JAR file is now available in the target directory and we may start up the application by executing the following command on the command line:

$ java -jar your-app.jar
https://www.baeldung.com/spring-boot-app-as-a-service




References --

1)https://www.datadoghq.com/blog/how-to-collect-cassandra-metrics/#collecting-metrics-with-jconsole

the recent read latency would be calculated from the deltas of those two metrics:

(ReadTotalLatency1−ReadTotalLatency0)/(ReadLatency1−ReadLatency0)

2)https://cassandra.apache.org/doc/latest/operating/metrics.html

3)https://community.datastax.com/questions/4969/identify-slow-queries.html#:~:text=Slow%20query%20logging%20was%20added,slow_query_log_timeout_in_ms%3A%20500

to collect slow query in cassandra
