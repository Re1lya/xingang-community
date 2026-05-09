package com.xingang.community.ai.agent;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.agent.dto.AgentChatResponse;
import com.xingang.community.ai.agent.dto.AgentSessionClearResponse;
import com.xingang.community.ai.agent.dto.KnowledgeRebuildResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentOrchestrationService {

    AgentChatResponse chat(AgentChatRequest request, Long userId, String principalKey);

    SseEmitter streamChat(AgentChatRequest request, Long userId, String principalKey);

    AgentSessionClearResponse clearSession(String conversationId, Long userId, String principalKey);

    KnowledgeRebuildResponse rebuildKnowledgeIndex(Long userId, String principalKey);
}
