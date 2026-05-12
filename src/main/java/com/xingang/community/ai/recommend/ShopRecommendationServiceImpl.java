package com.xingang.community.ai.recommend;

import com.xingang.community.ai.tool.model.ShopCandidate;
import com.xingang.community.shop.service.ShopService;
import com.xingang.community.vo.ShopTypeVO;
import com.xingang.community.vo.ShopVO;
import com.xingang.community.voucher.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShopRecommendationServiceImpl implements ShopRecommendationService {

    private static final int DEFAULT_LIMIT = 20;

    private final ShopService shopService;
    private final VoucherService voucherService;

    public ShopRecommendationServiceImpl(ShopService shopService, VoucherService voucherService) {
        this.shopService = shopService;
        this.voucherService = voucherService;
    }

    @Override
    public List<ShopCandidate> recommend(String city,
                                         String scenePreference,
                                         Integer budgetMax,
                                         Integer partySize,
                                         List<String> includedCategories,
                                         List<String> excludedCategories) {
        Map<Long, String> typeNameMap = resolveTypeNameMap();
        List<Long> typeIds = resolveTargetTypeIds(typeNameMap, includedCategories, excludedCategories);
        List<ShopVO> shops = queryShopsByTypeIds(typeIds, null, null, DEFAULT_LIMIT);
        return buildCandidates(shops, typeNameMap, city, budgetMax, DEFAULT_LIMIT);
    }

    @Override
    public List<ShopCandidate> recommendNearby(String city,
                                               Double longitude,
                                               Double latitude,
                                               Integer radiusMeters,
                                               Integer budgetMax,
                                               List<String> excludedCategories) {
        Map<Long, String> typeNameMap = resolveTypeNameMap();
        List<Long> typeIds = resolveTargetTypeIds(typeNameMap, List.of(), excludedCategories);
        List<ShopVO> shops = queryShopsByTypeIds(typeIds, longitude, latitude, DEFAULT_LIMIT);
        return buildNearbyCandidates(shops, typeNameMap, city, budgetMax, radiusMeters, DEFAULT_LIMIT);
    }

    private Map<Long, String> resolveTypeNameMap() {
        List<ShopTypeVO> shopTypes = shopService.queryShopTypeList();
        if (CollectionUtils.isEmpty(shopTypes)) {
            return Map.of();
        }
        return shopTypes.stream()
                .collect(Collectors.toMap(ShopTypeVO::getId, ShopTypeVO::getName, (a, b) -> a, LinkedHashMap::new));
    }

    private List<Long> resolveTargetTypeIds(Map<Long, String> typeNameMap,
                                            List<String> includedCategories,
                                            List<String> excludedCategories) {
        Set<Long> included = typeNameMap.entrySet().stream()
                .filter(entry -> matchesCategory(entry.getValue(), includedCategories))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<Long> excluded = typeNameMap.entrySet().stream()
                .filter(entry -> matchesCategory(entry.getValue(), excludedCategories))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Long> target;
        if (CollectionUtils.isEmpty(includedCategories)) {
            target = new ArrayList<>(typeNameMap.keySet());
        } else {
            target = new ArrayList<>(included);
        }
        target.removeAll(excluded);
        return target;
    }

    private List<ShopVO> queryShopsByTypeIds(List<Long> typeIds, Double longitude, Double latitude, int pageSize) {
        if (CollectionUtils.isEmpty(typeIds)) {
            return List.of();
        }
        List<ShopVO> shops = new ArrayList<>();
        for (Long typeId : typeIds) {
            shops.addAll(shopService.queryShopByType(typeId, 1, pageSize, longitude, latitude));
        }
        return shops.stream()
                .collect(Collectors.toMap(ShopVO::getId, shop -> shop, (first, second) -> first, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private List<ShopCandidate> buildCandidates(List<ShopVO> shops,
                                                Map<Long, String> typeNameMap,
                                                String city,
                                                Integer budgetMax,
                                                int limit) {
        Map<Long, Boolean> couponIndex = buildCouponIndex(shops);
        return shops.stream()
                .filter(shop -> matchesCity(shop, city))
                .filter(shop -> withinBudget(shop, budgetMax))
                .sorted(Comparator
                        .comparingInt((ShopVO shop) -> couponIndex.getOrDefault(shop.getId(), false) ? 1 : 0)
                        .reversed()
                        .thenComparing(ShopVO::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ShopVO::getAvgPrice, Comparator.nullsLast(Integer::compareTo)))
                .limit(limit)
                .map(shop -> toCandidate(shop, typeNameMap.get(shop.getTypeId()), city))
                .toList();
    }

    private List<ShopCandidate> buildNearbyCandidates(List<ShopVO> shops,
                                                      Map<Long, String> typeNameMap,
                                                      String city,
                                                      Integer budgetMax,
                                                      Integer radiusMeters,
                                                      int limit) {
        Map<Long, Boolean> couponIndex = buildCouponIndex(shops);
        return shops.stream()
                .filter(shop -> matchesCity(shop, city))
                .filter(shop -> withinBudget(shop, budgetMax))
                .filter(shop -> withinRadius(shop, radiusMeters))
                .sorted(Comparator
                        .comparing(ShopVO::getDistance, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(Comparator.comparingInt(
                                (ShopVO shop) -> couponIndex.getOrDefault(shop.getId(), false) ? 1 : 0
                        ).reversed())
                        .thenComparing(ShopVO::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ShopVO::getAvgPrice, Comparator.nullsLast(Integer::compareTo)))
                .limit(limit)
                .map(shop -> toCandidate(shop, typeNameMap.get(shop.getTypeId()), city))
                .toList();
    }

    private boolean matchesCategory(String typeName, List<String> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return false;
        }
        return categories.stream().anyMatch(category -> containsIgnoreCase(typeName, category));
    }

    private boolean matchesCity(ShopVO shop, String city) {
        if (!StringUtils.hasText(city)) {
            return true;
        }
        return containsIgnoreCase(shop.getArea(), city)
                || containsIgnoreCase(shop.getAddress(), city)
                || containsIgnoreCase(shop.getName(), city);
    }

    private boolean withinBudget(ShopVO shop, Integer budgetMax) {
        if (budgetMax == null || budgetMax <= 0) {
            return true;
        }
        return shop.getAvgPrice() != null && shop.getAvgPrice() <= budgetMax;
    }

    private boolean withinRadius(ShopVO shop, Integer radiusMeters) {
        if (radiusMeters == null || radiusMeters <= 0) {
            return true;
        }
        return shop.getDistance() != null && shop.getDistance() <= radiusMeters;
    }

    private Map<Long, Boolean> buildCouponIndex(List<ShopVO> shops) {
        Map<Long, Boolean> couponIndex = new LinkedHashMap<>();
        for (ShopVO shop : shops) {
            couponIndex.put(shop.getId(), !CollectionUtils.isEmpty(voucherService.queryVoucherByShopId(shop.getId())));
        }
        return couponIndex;
    }

    private ShopCandidate toCandidate(ShopVO shop, String category, String city) {
        ShopCandidate candidate = new ShopCandidate();
        candidate.setShopId(shop.getId());
        candidate.setName(shop.getName());
        candidate.setCity(StringUtils.hasText(city) ? city : shop.getArea());
        candidate.setCategory(category);
        candidate.setAveragePrice(shop.getAvgPrice());
        candidate.setDistanceKm(shop.getDistance() == null ? null : roundTo3(shop.getDistance() / 1000D));
        candidate.setRating(shop.getScore());
        return candidate;
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
}
