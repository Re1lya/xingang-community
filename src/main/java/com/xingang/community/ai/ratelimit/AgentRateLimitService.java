package com.xingang.community.ai.ratelimit;

public interface AgentRateLimitService {

    AgentRateLimitDecision checkLimit(String scene, String principalKey);
}
