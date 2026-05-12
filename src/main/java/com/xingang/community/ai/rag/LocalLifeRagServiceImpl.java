package com.xingang.community.ai.rag;

import com.xingang.community.ai.agent.dto.RetrievalHit;
import com.xingang.community.ai.planning.AgentExecutionPlan;
import com.xingang.community.ai.rag.model.AiKnowledgeDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalLifeRagServiceImpl implements LocalLifeRagService {

    private final AiKnowledgeService aiKnowledgeService;

    @Value("${ai.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${ai.knowledge.top-k:5}")
    private int topK;

    @Value("${ai.rag.similarity-threshold:0.65}")
    private double similarityThreshold;

    public LocalLifeRagServiceImpl(AiKnowledgeService aiKnowledgeService) {
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @Override
    public List<RetrievalHit> retrieve(String question, AgentExecutionPlan plan) {
        if (!ragEnabled || plan == null || !Boolean.TRUE.equals(plan.getNeedRag())) {
            return List.of();
        }
        List<AiKnowledgeDocument> docs = aiKnowledgeService.retrieveKnowledge(question, topK, similarityThreshold);
        return docs.stream().map(this::toHit).toList();
    }

    private RetrievalHit toHit(AiKnowledgeDocument doc) {
        RetrievalHit hit = new RetrievalHit();
        hit.setKnowledgeId(doc.getId());
        hit.setTitle(doc.getTitle());
        hit.setSnippet(trimSnippet(doc.getContent(), 120));
        hit.setScore(0.8D);
        return hit;
    }

    private String trimSnippet(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
