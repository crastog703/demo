package com.example.demo.model;

public class Latency {
    Long readLatency;
    Long totalReadLatency;

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    Long writeLatency;



    Long TotalWriteLatency;
    Long rangeLatency;
    Long epoch;

    public Long getReadLatency() {
        return readLatency;
    }

    public void setReadLatency(Long readLatency) {
        this.readLatency = readLatency;
    }

    public Long getTotalReadLatency() {
        return totalReadLatency;
    }

    public void setTotalReadLatency(Long totalReadLatency) {
        this.totalReadLatency = totalReadLatency;
    }

    public Long getWriteLatency() {
        return writeLatency;
    }

    public void setWriteLatency(Long writeLatency) {
        this.writeLatency = writeLatency;
    }

    public Long getTotalWriteLatency() {
        return TotalWriteLatency;
    }

    public void setTotalWriteLatency(Long totalWriteLatency) {
        TotalWriteLatency = totalWriteLatency;
    }

    public Long getRangeLatency() {
        return rangeLatency;
    }

    public void setRangeLatency(Long rangeLatency) {
        this.rangeLatency = rangeLatency;
    }

    public Long getTotalRangeLatency() {
        return totalRangeLatency;
    }

    public void setTotalRangeLatency(Long totalRangeLatency) {
        this.totalRangeLatency = totalRangeLatency;
    }

    Long totalRangeLatency;

}
