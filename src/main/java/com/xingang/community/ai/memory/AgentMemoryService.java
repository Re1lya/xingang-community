package com.xingang.community.ai.memory;

import java.util.List;

public interface AgentMemoryService {

    List<ChatMessage> getRecentMessages(String conversationId, int limit);

    void appendTurn(String conversationId, String userMessage, String assistantMessage);

    String getConversationSummary(String conversationId);

    void saveConversationSummary(String conversationId, String summary);

    List<UserFact> getUserFacts(Long userId);

    void upsertUserFact(Long userId, UserFact fact);

    void clearConversation(Long userId, String conversationId);
}
