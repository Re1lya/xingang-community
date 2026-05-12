package com.xingang.community.ai.tool;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.agent.dto.AgentToolTrace;
import com.xingang.community.ai.planning.AgentExecutionPlan;
import com.xingang.community.ai.recommend.ShopRecommendationService;
import com.xingang.community.ai.tool.model.BlogFact;
import com.xingang.community.ai.tool.model.CouponFact;
import com.xingang.community.ai.tool.model.RecommendationResult;
import com.xingang.community.ai.tool.model.ShopCandidate;
import com.xingang.community.ai.tool.model.ShopDetailFact;
import com.xingang.community.ai.tool.model.ToolCallEnvelope;
import com.xingang.community.ai.tool.model.UserLocationFact;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LocalLifeAgentToolsImpl implements LocalLifeAgentTools {

    private final ShopRecommendationService shopRecommendationService;

    public LocalLifeAgentToolsImpl(ShopRecommendationService shopRecommendationService) {
        this.shopRecommendationService = shopRecommendationService;
    }

    @Override
    public ToolCallEnvelope<List<ShopCandidate>> searchShops(String keyword, List<String> categories, String city, int limit) {
        return ToolCallEnvelope.of(TOOL_SEARCH_SHOPS, List.of(), 0);
    }

    @Override
    public ToolCallEnvelope<ShopDetailFact> getShopDetail(Long shopId) {
        ShopDetailFact fact = new ShopDetailFact();
        fact.setShopId(shopId);
        return ToolCallEnvelope.of(TOOL_GET_SHOP_DETAIL, fact, fact.getShopId() == null ? 0 : 1);
    }

    @Override
    public ToolCallEnvelope<List<CouponFact>> getShopCoupons(Long shopId) {
        return ToolCallEnvelope.of(TOOL_GET_SHOP_COUPONS, List.of(), 0);
    }

    @Override
    public ToolCallEnvelope<List<BlogFact>> getHotBlogs(String city, int limit) {
        return ToolCallEnvelope.of(TOOL_GET_HOT_BLOGS, List.of(), 0);
    }

    @Override
    public ToolCallEnvelope<UserLocationFact> getCurrentUserLocation(Long userId, String principalKey) {
        UserLocationFact location = new UserLocationFact();
        return ToolCallEnvelope.of(TOOL_GET_CURRENT_USER_LOCATION, location, 1);
    }

    @Override
    public ToolCallEnvelope<List<ShopCandidate>> recommendShops(String city,
                                                                String scenePreference,
                                                                Integer budgetMax,
                                                                Integer partySize,
                                                                List<String> includedCategories,
                                                                List<String> excludedCategories) {
        List<ShopCandidate> candidates = shopRecommendationService.recommend(
                city, scenePreference, budgetMax, partySize, includedCategories, excludedCategories
        );
        return ToolCallEnvelope.of(TOOL_RECOMMEND_SHOPS, candidates, candidates.size());
    }

    @Override
    public ToolCallEnvelope<RecommendationResult> recommendShopsV2(String city,
                                                                   String locationHint,
                                                                   Double longitude,
                                                                   Double latitude,
                                                                   Integer budgetMax,
                                                                   Integer partySize,
                                                                   List<String> includedCategories,
                                                                   List<String> excludedCategories) {
        RecommendationResult result = new RecommendationResult();
        result.setStrategy("skeleton-v2");
        result.setCandidates(shopRecommendationService.recommend(
                city, "recommendation", budgetMax, partySize, includedCategories, excludedCategories
        ));
        return ToolCallEnvelope.of(TOOL_RECOMMEND_SHOPS_V2, result, result.getCandidates().size());
    }

    @Override
    public ToolCallEnvelope<List<ShopCandidate>> recommendNearbyShops(String city,
                                                                      Double longitude,
                                                                      Double latitude,
                                                                      Integer radiusMeters,
                                                                      Integer budgetMax,
                                                                      List<String> excludedCategories) {
        List<ShopCandidate> candidates = shopRecommendationService.recommendNearby(
                city, longitude, latitude, radiusMeters, budgetMax, excludedCategories
        );
        return ToolCallEnvelope.of(TOOL_RECOMMEND_NEARBY_SHOPS, candidates, candidates.size());
    }

    @Override
    public List<AgentToolTrace> executePreferredTools(AgentExecutionPlan plan,
                                                      AgentChatRequest request,
                                                      Long userId,
                                                      String principalKey) {
        List<AgentToolTrace> traces = new ArrayList<>();
        if (plan == null || CollectionUtils.isEmpty(plan.getPreferredTools())) {
            return traces;
        }
        for (String toolName : plan.getPreferredTools()) {
            long start = System.currentTimeMillis();
            AgentToolTrace trace = new AgentToolTrace();
            trace.setToolName(toolName);
            trace.setInput(buildTraceInput(plan, request));
            trace.setSuccess(true);
            trace.setErrorCode(null);
            switch (toolName) {
                case TOOL_SEARCH_SHOPS -> trace.setOutputSize(
                        searchShops(request.getMessage(), plan.getIncludedCategories(), plan.getCity(), 10).getOutputSize()
                );
                case TOOL_GET_SHOP_DETAIL -> trace.setOutputSize(getShopDetail(null).getOutputSize());
                case TOOL_GET_SHOP_COUPONS -> trace.setOutputSize(getShopCoupons(null).getOutputSize());
                case TOOL_GET_HOT_BLOGS -> trace.setOutputSize(getHotBlogs(plan.getCity(), 5).getOutputSize());
                case TOOL_GET_CURRENT_USER_LOCATION -> trace.setOutputSize(getCurrentUserLocation(userId, principalKey).getOutputSize());
                case TOOL_RECOMMEND_SHOPS -> trace.setOutputSize(recommendShops(
                        plan.getCity(),
                        plan.getScenePreference(),
                        plan.getBudgetMax(),
                        plan.getPartySize(),
                        plan.getIncludedCategories(),
                        plan.getExcludedCategories()
                ).getOutputSize());
                case TOOL_RECOMMEND_SHOPS_V2 -> trace.setOutputSize(recommendShopsV2(
                        plan.getCity(),
                        plan.getLocationHint(),
                        request.getLongitude(),
                        request.getLatitude(),
                        plan.getBudgetMax(),
                        plan.getPartySize(),
                        plan.getIncludedCategories(),
                        plan.getExcludedCategories()
                ).getOutputSize());
                case TOOL_RECOMMEND_NEARBY_SHOPS -> trace.setOutputSize(recommendNearbyShops(
                        plan.getCity(),
                        request.getLongitude(),
                        request.getLatitude(),
                        3000,
                        plan.getBudgetMax(),
                        plan.getExcludedCategories()
                ).getOutputSize());
                default -> {
                    trace.setSuccess(false);
                    trace.setErrorCode("TOOL_UNSUPPORTED");
                    trace.setOutputSize(0);
                }
            }
            trace.setLatencyMs(System.currentTimeMillis() - start);
            traces.add(trace);
        }
        return traces;
    }

    private Map<String, Object> buildTraceInput(AgentExecutionPlan plan, AgentChatRequest request) {
        Map<String, Object> input = new HashMap<>();
        input.put("city", plan.getCity());
        input.put("locationHint", plan.getLocationHint());
        input.put("budgetMax", plan.getBudgetMax());
        input.put("partySize", plan.getPartySize());
        input.put("includedCategories", plan.getIncludedCategories());
        input.put("excludedCategories", plan.getExcludedCategories());
        input.put("longitude", request.getLongitude());
        input.put("latitude", request.getLatitude());
        return input;
    }
}
