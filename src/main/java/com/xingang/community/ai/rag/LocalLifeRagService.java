package com.xingang.community.ai.rag;

import com.xingang.community.ai.agent.dto.RetrievalHit;
import com.xingang.community.ai.planning.AgentExecutionPlan;

import java.util.List;

/**
 * RAG只用于静态知识（平台规则、FAQ、说明）。
 * 价格、库存、距离、优惠券、营业状态等动态事实必须走Tool或业务Service。
 */
public interface LocalLifeRagService {

    List<RetrievalHit> retrieve(String question, AgentExecutionPlan plan);
}
