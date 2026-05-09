package com.xingang.community.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisChatMemoryRepository {

    private static final String MEMORY_KEY_PREFIX = "ai:chat:memory:";
    private static final int DEFAULT_WINDOW_SIZE = 20;
    private static final Duration MEMORY_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ChatMessage> listRecentMessages(String conversationId, int limit) {
        String key = MEMORY_KEY_PREFIX + conversationId;
        long from = Math.max(-limit, -DEFAULT_WINDOW_SIZE);
        List<String> raw = stringRedisTemplate.opsForList().range(key, from, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> messages = new ArrayList<>(raw.size());
        for (String item : raw) {
            messages.add(readMessage(item));
        }
        return messages;
    }

    public void appendMessage(String conversationId, ChatMessage message) {
        String key = MEMORY_KEY_PREFIX + conversationId;
        String payload = writeMessage(message);
        stringRedisTemplate.opsForList().rightPush(key, payload);
        stringRedisTemplate.opsForList().trim(key, -DEFAULT_WINDOW_SIZE, -1);
        stringRedisTemplate.expire(key, MEMORY_TTL);
    }

    public void clearConversation(String conversationId) {
        stringRedisTemplate.delete(MEMORY_KEY_PREFIX + conversationId);
    }

    private String writeMessage(ChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize chat message", ex);
        }
    }

    private ChatMessage readMessage(String value) {
        try {
            return objectMapper.readValue(value, ChatMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to deserialize chat message", ex);
        }
    }
}
