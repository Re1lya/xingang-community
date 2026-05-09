package com.xingang.community.ai.rag;

import com.xingang.community.ai.rag.model.AiKnowledgeDocument;

import java.util.List;

public interface AiKnowledgeService {

    List<AiKnowledgeDocument> retrieveKnowledge(String query, int topK, double similarityThreshold);

    boolean rebuildKnowledgeIndex(String traceId);
}
