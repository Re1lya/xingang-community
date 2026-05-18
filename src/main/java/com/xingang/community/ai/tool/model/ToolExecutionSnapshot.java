package com.xingang.community.ai.tool.model;

import com.xingang.community.ai.agent.dto.AgentToolTrace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Structured facts produced during Tool execution.
 * Dynamic facts in final answers must come from this snapshot, not from RAG.
 */
public class ToolExecutionSnapshot {

    private List<AgentToolTrace> toolTrace = new ArrayList<>();
    private List<ShopCandidate> searchedCandidates = new ArrayList<>();
    private List<ShopCandidate> recommendedCandidates = new ArrayList<>();
    private ShopDetailFact shopDetail;
    private List<CouponFact> shopCoupons = new ArrayList<>();
    private UserLocationFact userLocation;
    private Long selectedShopId;
    private List<String> missingFacts = new ArrayList<>();
    private Set<String> executedTools = new LinkedHashSet<>();

    public List<AgentToolTrace> getToolTrace() {
        return toolTrace;
    }

    public void setToolTrace(List<AgentToolTrace> toolTrace) {
        this.toolTrace = toolTrace == null ? new ArrayList<>() : toolTrace;
    }

    public List<ShopCandidate> getSearchedCandidates() {
        return searchedCandidates;
    }

    public void setSearchedCandidates(List<ShopCandidate> searchedCandidates) {
        this.searchedCandidates = searchedCandidates == null ? new ArrayList<>() : searchedCandidates;
    }

    public List<ShopCandidate> getRecommendedCandidates() {
        return recommendedCandidates;
    }

    public void setRecommendedCandidates(List<ShopCandidate> recommendedCandidates) {
        this.recommendedCandidates = recommendedCandidates == null ? new ArrayList<>() : recommendedCandidates;
    }

    public ShopDetailFact getShopDetail() {
        return shopDetail;
    }

    public void setShopDetail(ShopDetailFact shopDetail) {
        this.shopDetail = shopDetail;
    }

    public List<CouponFact> getShopCoupons() {
        return shopCoupons;
    }

    public void setShopCoupons(List<CouponFact> shopCoupons) {
        this.shopCoupons = shopCoupons == null ? new ArrayList<>() : shopCoupons;
    }

    public UserLocationFact getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(UserLocationFact userLocation) {
        this.userLocation = userLocation;
    }

    public Long getSelectedShopId() {
        return selectedShopId;
    }

    public void setSelectedShopId(Long selectedShopId) {
        this.selectedShopId = selectedShopId;
    }

    public List<String> getMissingFacts() {
        return missingFacts;
    }

    public void setMissingFacts(List<String> missingFacts) {
        this.missingFacts = missingFacts == null ? new ArrayList<>() : missingFacts;
    }

    public void addMissingFact(String missingFact) {
        this.missingFacts.add(missingFact);
    }

    public Set<String> getExecutedTools() {
        return executedTools;
    }

    public void setExecutedTools(Set<String> executedTools) {
        this.executedTools = executedTools == null ? new LinkedHashSet<>() : executedTools;
    }

    public void markExecuted(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }
        executedTools.add(toolName);
    }

    public boolean wasExecuted(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }
        return executedTools.contains(toolName);
    }
}
