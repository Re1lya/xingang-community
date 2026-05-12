package com.xingang.community.ai.tool.model;

public class ToolCallEnvelope<T> {

    private String toolName;
    private T data;
    private int outputSize;

    public static <T> ToolCallEnvelope<T> of(String toolName, T data, int outputSize) {
        ToolCallEnvelope<T> envelope = new ToolCallEnvelope<>();
        envelope.setToolName(toolName);
        envelope.setData(data);
        envelope.setOutputSize(outputSize);
        return envelope;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }
}
