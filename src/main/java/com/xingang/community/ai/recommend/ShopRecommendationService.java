package com.xingang.community.ai.recommend;

import com.xingang.community.ai.tool.model.ShopCandidate;

import java.util.List;

public interface ShopRecommendationService {

    List<ShopCandidate> recommend(String city,
                                  String scenePreference,
                                  Integer budgetMax,
                                  Integer partySize,
                                  List<String> includedCategories,
                                  List<String> excludedCategories);

    List<ShopCandidate> recommendNearby(String city,
                                        Double longitude,
                                        Double latitude,
                                        Integer radiusMeters,
                                        Integer budgetMax,
                                        List<String> excludedCategories);
}
