package com.example.demo.model;

public class Latency {
    Double readLatency;
    Double totalReadLatency;

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    Double writeLatency;



    Double TotalWriteLatency;
    Double rangeLatency;
    Long epoch;

    public Double getReadLatency() {
        return readLatency;
    }

    public void setReadLatency(Double readLatency) {
        this.readLatency = readLatency;
    }

    public Double getTotalReadLatency() {
        return totalReadLatency;
    }

    public void setTotalReadLatency(Double totalReadLatency) {
        this.totalReadLatency = totalReadLatency;
    }

    public Double getWriteLatency() {
        return writeLatency;
    }

    public void setWriteLatency(Double writeLatency) {
        this.writeLatency = writeLatency;
    }

    public Double getTotalWriteLatency() {
        return TotalWriteLatency;
    }

    public void setTotalWriteLatency(Double totalWriteLatency) {
        TotalWriteLatency = totalWriteLatency;
    }

    public Double getRangeLatency() {
        return rangeLatency;
    }

    public void setRangeLatency(Double rangeLatency) {
        this.rangeLatency = rangeLatency;
    }

    public Double getTotalRangeLatency() {
        return totalRangeLatency;
    }

    public void setTotalRangeLatency(Double totalRangeLatency) {
        this.totalRangeLatency = totalRangeLatency;
    }

    Double totalRangeLatency;

}
