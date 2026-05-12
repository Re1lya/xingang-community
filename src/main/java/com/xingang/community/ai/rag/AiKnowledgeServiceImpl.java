package com.xingang.community.ai.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingang.community.ai.rag.model.AiKnowledgeDocument;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final AtomicReference<List<AiKnowledgeDocument>> knowledgeIndex = new AtomicReference<>(List.of());

    @Value("${ai.rag.store-file:classpath:knowledge/zyro-support.json}")
    private String storeFile;

    public AiKnowledgeServiceImpl(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        rebuildKnowledgeIndex("startup");
    }

    @Override
    public List<AiKnowledgeDocument> retrieveKnowledge(String query, int topK, double similarityThreshold) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return knowledgeIndex.get().stream()
                .map(doc -> new ScoredDocument(doc, score(normalized, doc)))
                .filter(scored -> scored.score >= similarityThreshold)
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .limit(Math.max(topK, 1))
                .map(ScoredDocument::document)
                .toList();
    }

    @Override
    public boolean rebuildKnowledgeIndex(String traceId) {
        try {
            List<AiKnowledgeDocument> loaded = loadKnowledgeDocuments();
            knowledgeIndex.set(loaded);
            log.info("traceId={} knowledge index rebuilt, size={}", traceId, loaded.size());
            return true;
        } catch (IOException ex) {
            log.error("traceId={} knowledge index rebuild failed", traceId, ex);
            return false;
        }
    }

    private List<AiKnowledgeDocument> loadKnowledgeDocuments() throws IOException {
        Resource resource = resourceLoader.getResource(storeFile);
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<AiKnowledgeDocument> docs = objectMapper.readValue(
                    inputStream, new TypeReference<List<AiKnowledgeDocument>>() {
                    });
            return docs == null ? List.of() : new ArrayList<>(docs);
        }
    }

    private double score(String query, AiKnowledgeDocument doc) {
        String title = safeLower(doc.getTitle());
        String content = safeLower(doc.getContent());
        if (title.contains(query)) {
            return 1.0;
        }
        if (content.contains(query)) {
            return 0.85;
        }
        int tokenHits = 0;
        String[] tokens = query.split("\\s+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (title.contains(token) || content.contains(token)) {
                tokenHits++;
            }
        }
        if (tokens.length == 0) {
            return 0.0;
        }
        return (double) tokenHits / tokens.length;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record ScoredDocument(AiKnowledgeDocument document, double score) {
    }
}
