package com.xingang.community.ai.planning;

import java.util.ArrayList;
import java.util.List;

public class AgentExecutionPlan {

    private String intent;
    private String city;
    private String locationHint;
    private Boolean nearby;
    private Integer partySize;
    private Integer budgetMax;
    private String scenePreference;
    private List<String> includedCategories = new ArrayList<>();
    private List<String> excludedCategories = new ArrayList<>();
    private List<String> preferredTools = new ArrayList<>();
    private Boolean needRag;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLocationHint() {
        return locationHint;
    }

    public void setLocationHint(String locationHint) {
        this.locationHint = locationHint;
    }

    public Boolean getNearby() {
        return nearby;
    }

    public void setNearby(Boolean nearby) {
        this.nearby = nearby;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public Integer getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Integer budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getScenePreference() {
        return scenePreference;
    }

    public void setScenePreference(String scenePreference) {
        this.scenePreference = scenePreference;
    }

    public List<String> getIncludedCategories() {
        return includedCategories;
    }

    public void setIncludedCategories(List<String> includedCategories) {
        this.includedCategories = includedCategories;
    }

    public List<String> getExcludedCategories() {
        return excludedCategories;
    }

    public void setExcludedCategories(List<String> excludedCategories) {
        this.excludedCategories = excludedCategories;
    }

    public List<String> getPreferredTools() {
        return preferredTools;
    }

    public void setPreferredTools(List<String> preferredTools) {
        this.preferredTools = preferredTools;
    }

    public Boolean getNeedRag() {
        return needRag;
    }

    public void setNeedRag(Boolean needRag) {
        this.needRag = needRag;
    }
}
