package com.xingang.community.ai.tool.model;

import java.util.ArrayList;
import java.util.List;

public class RecommendationResult {

    private String strategy;
    private List<ShopCandidate> candidates = new ArrayList<>();

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<ShopCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<ShopCandidate> candidates) {
        this.candidates = candidates;
    }
}
