package com.xingang.community.ai.agent.dto;

import com.xingang.community.ai.planning.AgentExecutionPlan;

import java.util.ArrayList;
import java.util.List;

public class AgentChatResponse {

    private String answer;
    private String conversationId;
    private String traceId;
    private AgentExecutionPlan plan;
    private List<AgentToolTrace> toolTrace = new ArrayList<>();
    private List<RetrievalHit> retrievalHits = new ArrayList<>();
    private Long latencyMs;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public AgentExecutionPlan getPlan() {
        return plan;
    }

    public void setPlan(AgentExecutionPlan plan) {
        this.plan = plan;
    }

    public List<AgentToolTrace> getToolTrace() {
        return toolTrace;
    }

    public void setToolTrace(List<AgentToolTrace> toolTrace) {
        this.toolTrace = toolTrace;
    }

    public List<RetrievalHit> getRetrievalHits() {
        return retrievalHits;
    }

    public void setRetrievalHits(List<RetrievalHit> retrievalHits) {
        this.retrievalHits = retrievalHits;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
