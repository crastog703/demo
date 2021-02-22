# demo
for cassandra to get read ,write and range latency ,gc pauses ,slow query and error rate





Refrences

1)https://www.datadoghq.com/blog/how-to-collect-cassandra-metrics/#collecting-metrics-with-jconsole

the recent read latency would be calculated from the deltas of those two metrics:

(ReadTotalLatency1−ReadTotalLatency0)/(ReadLatency1−ReadLatency0)

2)https://cassandra.apache.org/doc/latest/operating/metrics.html

3)https://community.datastax.com/questions/4969/identify-slow-queries.html#:~:text=Slow%20query%20logging%20was%20added,slow_query_log_timeout_in_ms%3A%20500

to collect slow query in cassandra
