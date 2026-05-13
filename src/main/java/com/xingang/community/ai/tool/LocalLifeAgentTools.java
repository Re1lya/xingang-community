package com.xingang.community.ai.tool;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.planning.AgentExecutionPlan;
import com.xingang.community.ai.tool.model.BlogFact;
import com.xingang.community.ai.tool.model.CouponFact;
import com.xingang.community.ai.tool.model.RecommendationResult;
import com.xingang.community.ai.tool.model.ShopCandidate;
import com.xingang.community.ai.tool.model.ShopDetailFact;
import com.xingang.community.ai.tool.model.ToolCallEnvelope;
import com.xingang.community.ai.tool.model.ToolExecutionSnapshot;
import com.xingang.community.ai.tool.model.UserLocationFact;

import java.util.List;

/**
 * Tool模块只承载动态事实查询适配，不负责自然语言生成。
 * 动态事实（价格、库存、距离、营业状态等）必须通过Tool查询。
 */
public interface LocalLifeAgentTools {

    String TOOL_SEARCH_SHOPS = "search_shops";
    String TOOL_GET_SHOP_DETAIL = "get_shop_detail";
    String TOOL_GET_SHOP_COUPONS = "get_shop_coupons";
    String TOOL_GET_HOT_BLOGS = "get_hot_blogs";
    String TOOL_GET_CURRENT_USER_LOCATION = "get_current_user_location";
    String TOOL_RECOMMEND_SHOPS = "recommend_shops";
    String TOOL_RECOMMEND_SHOPS_V2 = "recommend_shops_v2";
    String TOOL_RECOMMEND_NEARBY_SHOPS = "recommend_nearby_shops";

    ToolCallEnvelope<List<ShopCandidate>> searchShops(String keyword, List<String> categories, String city, int limit);

    ToolCallEnvelope<ShopDetailFact> getShopDetail(Long shopId);

    ToolCallEnvelope<List<CouponFact>> getShopCoupons(Long shopId);

    ToolCallEnvelope<List<BlogFact>> getHotBlogs(String city, int limit);

    ToolCallEnvelope<UserLocationFact> getCurrentUserLocation(Long userId, String principalKey);

    ToolCallEnvelope<List<ShopCandidate>> recommendShops(String city,
                                                         String scenePreference,
                                                         Integer budgetMax,
                                                         Integer partySize,
                                                         List<String> includedCategories,
                                                         List<String> excludedCategories);

    ToolCallEnvelope<RecommendationResult> recommendShopsV2(String city,
                                                            String locationHint,
                                                            Double longitude,
                                                            Double latitude,
                                                            Integer budgetMax,
                                                            Integer partySize,
                                                            List<String> includedCategories,
                                                            List<String> excludedCategories);

    ToolCallEnvelope<List<ShopCandidate>> recommendNearbyShops(String city,
                                                               Double longitude,
                                                               Double latitude,
                                                               Integer radiusMeters,
                                                               Integer budgetMax,
                                                               List<String> excludedCategories);

    ToolExecutionSnapshot executePreferredTools(AgentExecutionPlan plan,
                                                AgentChatRequest request,
                                                Long userId,
                                                String principalKey);
}
