package com.xingang.community.ai.memory;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RedisAgentMemoryServiceImpl implements AgentMemoryService {

    private final RedisChatMemoryRepository memoryRepository;
    private final RedisChatContextRepository contextRepository;

    public RedisAgentMemoryServiceImpl(RedisChatMemoryRepository memoryRepository,
                                       RedisChatContextRepository contextRepository) {
        this.memoryRepository = memoryRepository;
        this.contextRepository = contextRepository;
    }

    @Override
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        return memoryRepository.listRecentMessages(conversationId, limit);
    }

    @Override
    public void appendTurn(String conversationId, String userMessage, String assistantMessage) {
        ChatMessage user = new ChatMessage();
        user.setRole("user");
        user.setContent(userMessage);
        user.setTimestampEpochMs(Instant.now().toEpochMilli());
        memoryRepository.appendMessage(conversationId, user);

        ChatMessage assistant = new ChatMessage();
        assistant.setRole("assistant");
        assistant.setContent(assistantMessage);
        assistant.setTimestampEpochMs(Instant.now().toEpochMilli());
        memoryRepository.appendMessage(conversationId, assistant);
    }

    @Override
    public String getConversationSummary(String conversationId) {
        return contextRepository.getConversationSummary(conversationId);
    }

    @Override
    public void saveConversationSummary(String conversationId, String summary) {
        contextRepository.saveConversationSummary(conversationId, summary);
    }

    @Override
    public List<UserFact> getUserFacts(Long userId) {
        return contextRepository.listUserFacts(userId);
    }

    @Override
    public void upsertUserFact(Long userId, UserFact fact) {
        contextRepository.upsertUserFact(userId, fact);
    }

    @Override
    public void clearConversation(Long userId, String conversationId) {
        memoryRepository.clearConversation(conversationId);
        contextRepository.clearConversationSummary(conversationId);
    }
}
