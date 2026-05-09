package com.xingang.community.ai.memory;

public class ChatMessage {

    private String role;
    private String content;
    private long timestampEpochMs;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public void setTimestampEpochMs(long timestampEpochMs) {
        this.timestampEpochMs = timestampEpochMs;
    }
}
