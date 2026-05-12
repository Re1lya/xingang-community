package com.xingang.community.ai.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Service
public class AgentRateLimitServiceImpl implements AgentRateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ai.rate-limit.chat-limit:60}")
    private long chatLimit;

    @Value("${ai.rate-limit.stream-limit:30}")
    private long streamLimit;

    @Value("${ai.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public AgentRateLimitServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public AgentRateLimitDecision checkLimit(String scene, String principalKey) {
        String normalizedScene = StringUtils.hasText(scene) ? scene : "chat";
        String normalizedPrincipal = StringUtils.hasText(principalKey) ? principalKey : "anonymous";
        long limit = resolveLimit(normalizedScene);
        long bucket = Instant.now().getEpochSecond() / Math.max(windowSeconds, 1);
        String key = "ai:rate-limit:" + normalizedScene + ":" + normalizedPrincipal + ":" + bucket;
        Long used = stringRedisTemplate.opsForValue().increment(key);
        if (used != null && used == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 2));
        }

        long usedCount = used == null ? 0L : used;
        AgentRateLimitDecision decision = new AgentRateLimitDecision();
        decision.setAllowed(usedCount <= limit);
        decision.setLimit(limit);
        decision.setUsed(usedCount);
        decision.setRemaining(Math.max(0, limit - usedCount));
        decision.setKey(key);
        return decision;
    }

    private long resolveLimit(String scene) {
        if ("stream".equalsIgnoreCase(scene)) {
            return streamLimit;
        }
        return chatLimit;
    }
}
