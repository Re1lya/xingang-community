package com.xingang.community.ai.memory;

public class UserFact {

    private String factKey;
    private String factValue;
    private double confidence;
    private long updatedAtEpochMs;

    public String getFactKey() {
        return factKey;
    }

    public void setFactKey(String factKey) {
        this.factKey = factKey;
    }

    public String getFactValue() {
        return factValue;
    }

    public void setFactValue(String factValue) {
        this.factValue = factValue;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public long getUpdatedAtEpochMs() {
        return updatedAtEpochMs;
    }

    public void setUpdatedAtEpochMs(long updatedAtEpochMs) {
        this.updatedAtEpochMs = updatedAtEpochMs;
    }
}
