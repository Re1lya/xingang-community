package com.xingang.community.ai.recommend;

import com.xingang.community.ai.tool.model.ShopCandidate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopRecommendationServiceImpl implements ShopRecommendationService {

    @Override
    public List<ShopCandidate> recommend(String city,
                                         String scenePreference,
                                         Integer budgetMax,
                                         Integer partySize,
                                         List<String> includedCategories,
                                         List<String> excludedCategories) {
        return List.of();
    }

    @Override
    public List<ShopCandidate> recommendNearby(String city,
                                               Double longitude,
                                               Double latitude,
                                               Integer radiusMeters,
                                               Integer budgetMax,
                                               List<String> excludedCategories) {
        return List.of();
    }
}
