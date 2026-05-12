package com.xingang.community.ai.planning;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.memory.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentPlanningServiceImpl implements AgentPlanningService {

    private static final Pattern PARTY_SIZE_PATTERN = Pattern.compile("(\\d+)\\s*人");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("(\\d+)\\s*(元|块)");

    @Override
    public AgentExecutionPlan plan(AgentChatRequest request, List<ChatMessage> recentMessages) {
        String message = request.getMessage();
        AgentExecutionPlan plan = new AgentExecutionPlan();
        plan.setIntent(resolveIntent(request.getScene(), message));
        plan.setCity(request.getCity());
        plan.setLocationHint(resolveLocationHint(message, recentMessages));
        plan.setNearby(message != null && message.contains("附近"));
        plan.setPartySize(extractInt(PARTY_SIZE_PATTERN, message));
        plan.setBudgetMax(extractInt(BUDGET_PATTERN, message));
        plan.setScenePreference(resolveScenePreference(message));
        plan.setIncludedCategories(resolveIncludedCategories(message));
        plan.setExcludedCategories(resolveExcludedCategories(message));
        plan.setPreferredTools(resolvePreferredTools(plan));
        plan.setNeedRag(needRag(plan, message));
        return plan;
    }

    private String resolveIntent(String scene, String message) {
        if (StringUtils.hasText(scene)) {
            return scene.trim().toLowerCase(Locale.ROOT);
        }
        if (!StringUtils.hasText(message)) {
            return "support";
        }
        if (message.contains("推荐") || message.contains("附近")) {
            return "recommendation";
        }
        if (message.contains("规则") || message.contains("为什么") || message.contains("说明")) {
            return "support";
        }
        return "fact_query";
    }

    private Integer extractInt(Pattern pattern, String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String resolveLocationHint(String message, List<ChatMessage> recentMessages) {
        if (StringUtils.hasText(message) && message.contains("附近")) {
            int index = message.indexOf("附近");
            if (index > 0) {
                return message.substring(0, index).trim();
            }
        }
        if (recentMessages == null || recentMessages.isEmpty()) {
            return null;
        }
        ChatMessage latest = recentMessages.get(recentMessages.size() - 1);
        return latest.getContent();
    }

    private String resolveScenePreference(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        if (message.contains("约会")) {
            return "约会";
        }
        if (message.contains("聚餐")) {
            return "聚餐";
        }
        if (message.contains("家庭")) {
            return "家庭";
        }
        return null;
    }

    private List<String> resolveIncludedCategories(String message) {
        List<String> categories = new ArrayList<>();
        if (!StringUtils.hasText(message)) {
            return categories;
        }
        if (message.contains("粤菜")) {
            categories.add("粤菜");
        }
        if (message.contains("西餐")) {
            categories.add("西餐");
        }
        return categories;
    }

    private List<String> resolveExcludedCategories(String message) {
        List<String> categories = new ArrayList<>();
        if (!StringUtils.hasText(message)) {
            return categories;
        }
        if (message.contains("不要火锅")) {
            categories.add("火锅");
        }
        if (message.contains("不要西餐")) {
            categories.add("西餐");
        }
        return categories;
    }

    private List<String> resolvePreferredTools(AgentExecutionPlan plan) {
        List<String> tools = new ArrayList<>();
        if ("recommendation".equals(plan.getIntent())) {
            if (Boolean.TRUE.equals(plan.getNearby())) {
                tools.add("recommend_nearby_shops");
            }
            tools.add("recommend_shops_v2");
            tools.add("get_shop_coupons");
            return tools;
        }
        if ("fact_query".equals(plan.getIntent())) {
            tools.add("search_shops");
            tools.add("get_shop_detail");
            return tools;
        }
        tools.add("search_shops");
        return tools;
    }

    private boolean needRag(AgentExecutionPlan plan, String message) {
        if ("support".equals(plan.getIntent())) {
            return true;
        }
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("规则") || message.contains("说明") || message.contains("为什么");
    }
}
