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
import com.xingang.community.shop.service.ShopService;
import com.xingang.community.vo.ShopTypeVO;
import com.xingang.community.vo.ShopVO;
import com.xingang.community.vo.VoucherVO;
import com.xingang.community.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalLifeAgentToolsImpl implements LocalLifeAgentTools {

    private static final Logger log = LoggerFactory.getLogger(LocalLifeAgentToolsImpl.class);
    private static final int SHOP_ID_PRIORITY_NONE = 0;
    private static final int SHOP_ID_PRIORITY_SEARCH = 1;
    private static final int SHOP_ID_PRIORITY_RECOMMEND = 2;
    private static final int SHOP_ID_PRIORITY_EXPLICIT = 3;
    private static final Pattern EXPLICIT_SHOP_ID_PATTERN = Pattern.compile("(?:shop\\s*id|shopId|shop_id|商户\\s*id|店铺\\s*id|店铺ID|商户ID)\\s*[:：=]?\\s*(\\d+)");

    private final ShopService shopService;
    private final VoucherService voucherService;
    private final ShopRecommendationService shopRecommendationService;

    public LocalLifeAgentToolsImpl(ShopService shopService,
                                   VoucherService voucherService,
                                   ShopRecommendationService shopRecommendationService) {
        this.shopService = shopService;
        this.voucherService = voucherService;
        this.shopRecommendationService = shopRecommendationService;
    }

    @Override
    public ToolCallEnvelope<List<ShopCandidate>> searchShops(String keyword, List<String> categories, String city, int limit) {
        int normalizedLimit = Math.max(limit, 1);
        Map<Long, String> typeNameMap = resolveTypeNameMap();
        List<Long> typeIds = resolveTypeIds(typeNameMap, categories);
        if (CollectionUtils.isEmpty(typeIds)) {
            typeIds = new ArrayList<>(typeNameMap.keySet());
        }

        List<ShopVO> candidates = new ArrayList<>();
        for (Long typeId : typeIds) {
            candidates.addAll(shopService.queryShopByType(typeId, 1, normalizedLimit, null, null));
        }

        List<ShopCandidate> result = candidates.stream()
                .filter(shop -> matchesKeyword(shop, keyword))
                .filter(shop -> matchesCity(shop, city))
                .sorted(Comparator.comparing(ShopVO::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizedLimit)
                .map(shop -> toShopCandidate(shop, typeNameMap.get(shop.getTypeId()), city))
                .toList();
        return ToolCallEnvelope.of(TOOL_SEARCH_SHOPS, result, result.size());
    }

    @Override
    public ToolCallEnvelope<ShopDetailFact> getShopDetail(Long shopId) {
        if (shopId == null) {
            return ToolCallEnvelope.of(TOOL_GET_SHOP_DETAIL, new ShopDetailFact(), 0);
        }
        ShopVO shop = shopService.queryById(shopId);
        if (shop == null) {
            return ToolCallEnvelope.of(TOOL_GET_SHOP_DETAIL, new ShopDetailFact(), 0);
        }
        ShopDetailFact fact = new ShopDetailFact();
        fact.setShopId(shop.getId());
        fact.setName(shop.getName());
        fact.setAveragePrice(shop.getAvgPrice());
        fact.setRating(shop.getScore());
        fact.setAddress(shop.getAddress());
        fact.setBusinessHours(shop.getOpenHours());
        fact.setBusinessStatus(resolveBusinessStatus(shop.getOpenHours()));
        return ToolCallEnvelope.of(TOOL_GET_SHOP_DETAIL, fact, 1);
    }

    @Override
    public ToolCallEnvelope<List<CouponFact>> getShopCoupons(Long shopId) {
        if (shopId == null) {
            return ToolCallEnvelope.of(TOOL_GET_SHOP_COUPONS, List.of(), 0);
        }
        List<VoucherVO> vouchers = voucherService.queryVoucherByShopId(shopId);
        List<CouponFact> coupons = vouchers.stream().map(this::toCouponFact).toList();
        return ToolCallEnvelope.of(TOOL_GET_SHOP_COUPONS, coupons, coupons.size());
    }

    @Override
    public ToolCallEnvelope<List<BlogFact>> getHotBlogs(String city, int limit) {
        // 当前阶段未接入博客业务Service，保持结构化空结果并在工具轨迹里显式标识。
        log.info("Tool {} returns empty because blog service is not wired yet, city={}", TOOL_GET_HOT_BLOGS, city);
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
        List<ShopCandidate> candidates;
        if (longitude != null && latitude != null) {
            candidates = shopRecommendationService.recommendNearby(
                    city, longitude, latitude, 3000, budgetMax, excludedCategories
            );
            result.setStrategy("nearby-v2");
        } else {
            candidates = shopRecommendationService.recommend(
                    city, "recommendation", budgetMax, partySize, includedCategories, excludedCategories
            );
            result.setStrategy("city-v2");
        }
        result.setCandidates(candidates);
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
        SelectedShopContext selectedShopContext = initSelectedShopContext(request);
        for (String toolName : plan.getPreferredTools()) {
            long start = System.currentTimeMillis();
            AgentToolTrace trace = new AgentToolTrace();
            trace.setToolName(toolName);
            trace.setInput(buildTraceInput(plan, request));
            trace.setSuccess(true);
            trace.setErrorCode(null);
            switch (toolName) {
                case TOOL_SEARCH_SHOPS -> {
                    ToolCallEnvelope<List<ShopCandidate>> envelope = searchShops(
                            request.getMessage(), plan.getIncludedCategories(), plan.getCity(), 10
                    );
                    trace.setOutputSize(envelope.getOutputSize());
                    selectedShopContext = updateSelectedShopContext(
                            selectedShopContext,
                            extractTopShopId(envelope),
                            SHOP_ID_PRIORITY_SEARCH
                    );
                }
                case TOOL_GET_SHOP_DETAIL -> {
                    ToolCallEnvelope<ShopDetailFact> envelope = getShopDetail(selectedShopContext.shopId());
                    trace.setOutputSize(envelope.getOutputSize());
                    if (selectedShopContext.shopId() == null) {
                        trace.setSuccess(false);
                        trace.setErrorCode("SHOP_ID_MISSING");
                    }
                }
                case TOOL_GET_SHOP_COUPONS -> {
                    ToolCallEnvelope<List<CouponFact>> envelope = getShopCoupons(selectedShopContext.shopId());
                    trace.setOutputSize(envelope.getOutputSize());
                    if (selectedShopContext.shopId() == null) {
                        trace.setSuccess(false);
                        trace.setErrorCode("SHOP_ID_MISSING");
                    }
                }
                case TOOL_GET_HOT_BLOGS -> {
                    trace.setOutputSize(getHotBlogs(plan.getCity(), 5).getOutputSize());
                    trace.setSuccess(false);
                    trace.setErrorCode("BLOG_SERVICE_NOT_READY");
                }
                case TOOL_GET_CURRENT_USER_LOCATION -> trace.setOutputSize(getCurrentUserLocation(userId, principalKey).getOutputSize());
                case TOOL_RECOMMEND_SHOPS -> {
                    ToolCallEnvelope<List<ShopCandidate>> envelope = recommendShops(
                            plan.getCity(),
                            plan.getScenePreference(),
                            plan.getBudgetMax(),
                            plan.getPartySize(),
                            plan.getIncludedCategories(),
                            plan.getExcludedCategories()
                    );
                    trace.setOutputSize(envelope.getOutputSize());
                    selectedShopContext = updateSelectedShopContext(
                            selectedShopContext,
                            extractTopShopId(envelope),
                            SHOP_ID_PRIORITY_RECOMMEND
                    );
                }
                case TOOL_RECOMMEND_SHOPS_V2 -> {
                    ToolCallEnvelope<RecommendationResult> envelope = recommendShopsV2(
                            plan.getCity(),
                            plan.getLocationHint(),
                            request.getLongitude(),
                            request.getLatitude(),
                            plan.getBudgetMax(),
                            plan.getPartySize(),
                            plan.getIncludedCategories(),
                            plan.getExcludedCategories()
                    );
                    trace.setOutputSize(envelope.getOutputSize());
                    selectedShopContext = updateSelectedShopContext(
                            selectedShopContext,
                            extractTopShopId(envelope),
                            SHOP_ID_PRIORITY_RECOMMEND
                    );
                }
                case TOOL_RECOMMEND_NEARBY_SHOPS -> {
                    ToolCallEnvelope<List<ShopCandidate>> envelope = recommendNearbyShops(
                            plan.getCity(),
                            request.getLongitude(),
                            request.getLatitude(),
                            3000,
                            plan.getBudgetMax(),
                            plan.getExcludedCategories()
                    );
                    trace.setOutputSize(envelope.getOutputSize());
                    selectedShopContext = updateSelectedShopContext(
                            selectedShopContext,
                            extractTopShopId(envelope),
                            SHOP_ID_PRIORITY_RECOMMEND
                    );
                }
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

    private Map<Long, String> resolveTypeNameMap() {
        List<ShopTypeVO> types = shopService.queryShopTypeList();
        if (CollectionUtils.isEmpty(types)) {
            return Map.of();
        }
        return types.stream()
                .collect(Collectors.toMap(ShopTypeVO::getId, ShopTypeVO::getName, (a, b) -> a, LinkedHashMap::new));
    }

    private List<Long> resolveTypeIds(Map<Long, String> typeNameMap, List<String> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return List.of();
        }
        return typeNameMap.entrySet().stream()
                .filter(entry -> categories.stream().anyMatch(category -> containsIgnoreCase(entry.getValue(), category)))
                .map(Map.Entry::getKey)
                .toList();
    }

    private boolean matchesKeyword(ShopVO shop, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(shop.getName(), keyword)
                || containsIgnoreCase(shop.getArea(), keyword)
                || containsIgnoreCase(shop.getAddress(), keyword);
    }

    private boolean matchesCity(ShopVO shop, String city) {
        if (!StringUtils.hasText(city)) {
            return true;
        }
        return containsIgnoreCase(shop.getArea(), city) || containsIgnoreCase(shop.getAddress(), city);
    }

    private ShopCandidate toShopCandidate(ShopVO shop, String category, String city) {
        ShopCandidate candidate = new ShopCandidate();
        candidate.setShopId(shop.getId());
        candidate.setName(shop.getName());
        candidate.setCity(StringUtils.hasText(city) ? city : shop.getArea());
        candidate.setCategory(category);
        candidate.setAveragePrice(shop.getAvgPrice());
        candidate.setRating(shop.getScore());
        candidate.setDistanceKm(shop.getDistance() == null ? null : roundTo3(shop.getDistance() / 1000D));
        return candidate;
    }

    private CouponFact toCouponFact(VoucherVO voucher) {
        CouponFact fact = new CouponFact();
        fact.setVoucherId(voucher.getId());
        fact.setTitle(voucher.getTitle());
        fact.setPayValue(voucher.getPayValue());
        fact.setActualValue(voucher.getActualValue());
        fact.setDiscountAmount(resolveEstimatedSavings(voucher.getPayValue(), voucher.getActualValue()));
        fact.setValueDescription(buildVoucherValueDescription(voucher.getPayValue(), voucher.getActualValue()));
        fact.setStock(voucher.getStock());
        fact.setValidTimeRange(buildVoucherRange(voucher));
        return fact;
    }

    private Integer resolveEstimatedSavings(Long payValue, Long actualValue) {
        if (payValue == null || actualValue == null) {
            return null;
        }
        if (actualValue < payValue) {
            return null;
        }
        long savings = actualValue - payValue;
        return (int) Math.min(savings, Integer.MAX_VALUE);
    }

    private String buildVoucherRange(VoucherVO voucher) {
        if (voucher.getBeginTime() == null || voucher.getEndTime() == null) {
            return "N/A";
        }
        return voucher.getBeginTime() + " ~ " + voucher.getEndTime();
    }

    private String buildVoucherValueDescription(Long payValue, Long actualValue) {
        if (payValue == null || actualValue == null) {
            return "金额信息不完整";
        }
        return "payValue(应付,分)=" + payValue + ", actualValue(券面价值,分)=" + actualValue;
    }

    private String resolveBusinessStatus(String openHours) {
        if (!StringUtils.hasText(openHours) || !openHours.contains("-")) {
            return "UNKNOWN";
        }
        String[] range = openHours.split("-");
        if (range.length != 2) {
            return "UNKNOWN";
        }
        try {
            LocalTime start = LocalTime.parse(range[0].trim());
            LocalTime end = LocalTime.parse(range[1].trim());
            LocalTime now = LocalDateTime.now().toLocalTime();
            if (!now.isBefore(start) && !now.isAfter(end)) {
                return "OPEN";
            }
            return "CLOSED";
        } catch (DateTimeParseException ex) {
            return "UNKNOWN";
        }
    }

    private boolean containsIgnoreCase(String raw, String expected) {
        if (!StringUtils.hasText(raw) || !StringUtils.hasText(expected)) {
            return false;
        }
        return raw.toLowerCase().contains(expected.toLowerCase());
    }

    private double roundTo3(double value) {
        return Math.round(value * 1000D) / 1000D;
    }

    private SelectedShopContext initSelectedShopContext(AgentChatRequest request) {
        Long explicitShopId = extractUserExplicitShopId(request);
        if (explicitShopId != null) {
            return new SelectedShopContext(explicitShopId, SHOP_ID_PRIORITY_EXPLICIT);
        }
        return new SelectedShopContext(null, SHOP_ID_PRIORITY_NONE);
    }

    private SelectedShopContext updateSelectedShopContext(SelectedShopContext current,
                                                          Long candidateShopId,
                                                          int candidatePriority) {
        if (candidateShopId == null) {
            return current;
        }
        if (current.shopId() == null || candidatePriority > current.priority()) {
            return new SelectedShopContext(candidateShopId, candidatePriority);
        }
        return current;
    }

    private Long extractTopShopId(ToolCallEnvelope<List<ShopCandidate>> envelope) {
        if (envelope == null) {
            return null;
        }
        return extractTopShopId(envelope.getData());
    }

    private Long extractTopShopId(ToolCallEnvelope<RecommendationResult> envelope) {
        if (envelope == null || envelope.getData() == null) {
            return null;
        }
        return extractTopShopId(envelope.getData().getCandidates());
    }

    private Long extractTopShopId(List<ShopCandidate> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        return candidates.stream()
                .map(ShopCandidate::getShopId)
                .filter(shopId -> shopId != null && shopId > 0)
                .findFirst()
                .orElse(null);
    }

    private Long extractUserExplicitShopId(AgentChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return null;
        }
        Matcher matcher = EXPLICIT_SHOP_ID_PATTERN.matcher(request.getMessage());
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            log.warn("invalid explicit shopId in user message: {}", request.getMessage(), ex);
            return null;
        }
    }

    private record SelectedShopContext(Long shopId, int priority) {
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
