package com.xingang.community.ai.planning;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.memory.ChatMessage;

import java.util.List;

public interface AgentPlanningService {

    AgentExecutionPlan plan(AgentChatRequest request, List<ChatMessage> recentMessages);
}
