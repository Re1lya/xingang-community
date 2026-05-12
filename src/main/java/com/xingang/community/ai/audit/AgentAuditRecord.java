package com.xingang.community.ai.audit;

import com.xingang.community.ai.agent.dto.AgentToolTrace;

import java.util.ArrayList;
import java.util.List;

public class AgentAuditRecord {

    private String traceId;
    private Long userId;
    private String principalKey;
    private String conversationId;
    private String intent;
    private Integer retrievalCount;
    private List<AgentToolTrace> toolTrace = new ArrayList<>();
    private Long latencyMs;
    private String scene;
    private Long timestampEpochMs;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public void setPrincipalKey(String principalKey) {
        this.principalKey = principalKey;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Integer getRetrievalCount() {
        return retrievalCount;
    }

    public void setRetrievalCount(Integer retrievalCount) {
        this.retrievalCount = retrievalCount;
    }

    public List<AgentToolTrace> getToolTrace() {
        return toolTrace;
    }

    public void setToolTrace(List<AgentToolTrace> toolTrace) {
        this.toolTrace = toolTrace;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public void setTimestampEpochMs(Long timestampEpochMs) {
        this.timestampEpochMs = timestampEpochMs;
    }
}
