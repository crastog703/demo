package com.example.demo.utility;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String COUNT = "Count";
    public static final String MEAN_RATE = "MeanRate";
    public static final String PREVIOUS_WRITE_FAILURE = "previousWriteFailure";
    public static final String PREVIOUS_READ_FAILURE = "previousReadFailure";
    public static final String EPOCH = "epoch";
    public static final String PREVIOUS = "previous";
    public static final String PREVIOUS_CONCURRENT_MARK_SWEEP = "PreviousConcurrentMarkSweep";
    public static final String PREVIOUSPAR_NEW = "PreviousparNew";
    public static final String PREVIOUS_WRITE_LATENCY = "PreviousWriteLatency";
    public static final String PREVIOUS_READ_LATENCY = "PreviousReadLatency";
    public static final String PREVIOUS_TOTAL_WRITE_LATENCY = "PreviousTotalWriteLatency";
    public static final String PREVIOUS_RANGE_LATENCY = "PreviousRangeLatency";
    public static final String PREVIOUS_TOTAL_RANGE_LATENCY = "PreviousTotalRangeLatency";
    public static final String PREVIOUS_TOTAL_READ_LATENCY = "PreviousTotalReadLatency";
    public static final Map<String,Double> TIME_UNIT =new HashMap<>();
    static {
        TIME_UNIT.put("ms",1d);
        TIME_UNIT.put("second",.001d);
        TIME_UNIT.put("minute",.00000166);
    }
}
