package com.xingang.community.ai.agent.dto;

import java.util.Map;

public class AgentToolTrace {

    private String toolName;
    private Map<String, Object> input;
    private Integer outputSize;
    private Boolean success;
    private String errorCode;
    private Long latencyMs;

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public Integer getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(Integer outputSize) {
        this.outputSize = outputSize;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}
