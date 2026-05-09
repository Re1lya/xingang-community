package com.xingang.community.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class RedisChatContextRepository {

    private static final String CONTEXT_KEY_PREFIX = "ai:chat:context:";
    private static final String FACT_KEY_PREFIX = "ai:user:fact:";
    private static final Duration CONTEXT_TTL = Duration.ofDays(14);
    private static final Duration FACT_TTL = Duration.ofDays(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatContextRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public String getConversationSummary(String conversationId) {
        return stringRedisTemplate.opsForValue().get(CONTEXT_KEY_PREFIX + conversationId);
    }

    public void saveConversationSummary(String conversationId, String summary) {
        String key = CONTEXT_KEY_PREFIX + conversationId;
        stringRedisTemplate.opsForValue().set(key, summary);
        stringRedisTemplate.expire(key, CONTEXT_TTL);
    }

    public List<UserFact> listUserFacts(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(FACT_KEY_PREFIX + userId);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<UserFact> facts = new ArrayList<>(entries.size());
        for (Object value : entries.values()) {
            facts.add(readFact(String.valueOf(value)));
        }
        return facts;
    }

    public void upsertUserFact(Long userId, UserFact fact) {
        if (userId == null || fact == null || fact.getFactKey() == null) {
            return;
        }
        String key = FACT_KEY_PREFIX + userId;
        stringRedisTemplate.opsForHash().put(key, fact.getFactKey(), writeFact(fact));
        stringRedisTemplate.expire(key, FACT_TTL);
    }

    public void clearConversationSummary(String conversationId) {
        stringRedisTemplate.delete(CONTEXT_KEY_PREFIX + conversationId);
    }

    private String writeFact(UserFact fact) {
        try {
            return objectMapper.writeValueAsString(fact);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize user fact", ex);
        }
    }

    private UserFact readFact(String value) {
        try {
            return objectMapper.readValue(value, UserFact.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to deserialize user fact", ex);
        }
    }
}
