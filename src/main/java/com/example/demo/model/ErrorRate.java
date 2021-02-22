package com.example.demo.model;

public class ErrorRate {
    Double meanReadErrorRate;
    Long readError;
    Long writeError;

    public Double getMeanReadErrorRate() {
        return meanReadErrorRate;
    }

    public void setMeanReadErrorRate(Double meanReadErrorRate) {
        this.meanReadErrorRate = meanReadErrorRate;
    }

    public Long getReadError() {
        return readError;
    }

    public void setReadError(Long readError) {
        this.readError = readError;
    }

    public Long getWriteError() {
        return writeError;
    }

    public void setWriteError(Long writeError) {
        this.writeError = writeError;
    }

    public Double getMeanWriteErrorRate() {
        return meanWriteErrorRate;
    }

    public void setMeanWriteErrorRate(Double meanWriteErrorRate) {
        this.meanWriteErrorRate = meanWriteErrorRate;
    }

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    Double meanWriteErrorRate;
    Long epoch;
}
