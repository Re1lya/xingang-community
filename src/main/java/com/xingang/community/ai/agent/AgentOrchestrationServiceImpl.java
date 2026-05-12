package com.xingang.community.ai.agent;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.agent.dto.AgentChatResponse;
import com.xingang.community.ai.agent.dto.AgentSessionClearResponse;
import com.xingang.community.ai.agent.dto.AgentToolTrace;
import com.xingang.community.ai.agent.dto.KnowledgeRebuildResponse;
import com.xingang.community.ai.agent.dto.RetrievalHit;
import com.xingang.community.ai.agent.dto.sse.AgentSseChunkEvent;
import com.xingang.community.ai.agent.dto.sse.AgentSseDoneEvent;
import com.xingang.community.ai.agent.dto.sse.AgentSseErrorEvent;
import com.xingang.community.ai.agent.dto.sse.AgentSseMetaEvent;
import com.xingang.community.ai.audit.AgentAuditRecord;
import com.xingang.community.ai.audit.AgentAuditService;
import com.xingang.community.ai.memory.AgentMemoryService;
import com.xingang.community.ai.memory.ChatMessage;
import com.xingang.community.ai.planning.AgentExecutionPlan;
import com.xingang.community.ai.planning.AgentPlanningService;
import com.xingang.community.ai.rag.AiKnowledgeService;
import com.xingang.community.ai.rag.LocalLifeRagService;
import com.xingang.community.ai.ratelimit.AgentRateLimitDecision;
import com.xingang.community.ai.ratelimit.AgentRateLimitService;
import com.xingang.community.ai.tool.LocalLifeAgentTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
public class AgentOrchestrationServiceImpl implements AgentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrationServiceImpl.class);

    private final AgentPlanningService planningService;
    private final LocalLifeAgentTools localLifeAgentTools;
    private final LocalLifeRagService localLifeRagService;
    private final AiKnowledgeService aiKnowledgeService;
    private final AgentMemoryService memoryService;
    private final AgentRateLimitService rateLimitService;
    private final AgentAuditService auditService;

    public AgentOrchestrationServiceImpl(AgentPlanningService planningService,
                                         LocalLifeAgentTools localLifeAgentTools,
                                         LocalLifeRagService localLifeRagService,
                                         AiKnowledgeService aiKnowledgeService,
                                         AgentMemoryService memoryService,
                                         AgentRateLimitService rateLimitService,
                                         AgentAuditService auditService) {
        this.planningService = planningService;
        this.localLifeAgentTools = localLifeAgentTools;
        this.localLifeRagService = localLifeRagService;
        this.aiKnowledgeService = aiKnowledgeService;
        this.memoryService = memoryService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request, Long userId, String principalKey) {
        return doChat(request, userId, principalKey, "chat");
    }

    @Override
    public SseEmitter streamChat(AgentChatRequest request, Long userId, String principalKey) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> pushStreamEvents(emitter, request, userId, principalKey));
        return emitter;
    }

    @Override
    public AgentSessionClearResponse clearSession(String conversationId, Long userId, String principalKey) {
        String traceId = newTraceId();
        String resolvedPrincipal = resolvePrincipalKey(userId, principalKey);
        String resolvedConversationId = StringUtils.hasText(conversationId) ? conversationId : "default";
        memoryService.clearConversation(userId, resolvedConversationId);

        AgentAuditRecord record = new AgentAuditRecord();
        record.setTraceId(traceId);
        record.setUserId(userId);
        record.setPrincipalKey(resolvedPrincipal);
        record.setConversationId(resolvedConversationId);
        record.setIntent("clear_session");
        record.setRetrievalCount(0);
        record.setToolTrace(List.of());
        record.setLatencyMs(0L);
        record.setScene("session");
        record.setTimestampEpochMs(Instant.now().toEpochMilli());
        auditService.publish(record);

        AgentSessionClearResponse response = new AgentSessionClearResponse();
        response.setSuccess(true);
        response.setConversationId(resolvedConversationId);
        response.setTraceId(traceId);
        return response;
    }

    @Override
    public KnowledgeRebuildResponse rebuildKnowledgeIndex(Long userId, String principalKey) {
        String traceId = newTraceId();
        String resolvedPrincipal = resolvePrincipalKey(userId, principalKey);
        boolean success = aiKnowledgeService.rebuildKnowledgeIndex(traceId);

        AgentAuditRecord record = new AgentAuditRecord();
        record.setTraceId(traceId);
        record.setUserId(userId);
        record.setPrincipalKey(resolvedPrincipal);
        record.setConversationId("knowledge-rebuild");
        record.setIntent("knowledge_rebuild");
        record.setRetrievalCount(0);
        record.setToolTrace(List.of());
        record.setLatencyMs(0L);
        record.setScene("knowledge");
        record.setTimestampEpochMs(Instant.now().toEpochMilli());
        auditService.publish(record);

        KnowledgeRebuildResponse response = new KnowledgeRebuildResponse();
        response.setSuccess(success);
        response.setTraceId(traceId);
        response.setMessage(success ? "knowledge index rebuild started" : "knowledge index rebuild failed");
        return response;
    }

    private AgentChatResponse doChat(AgentChatRequest request, Long userId, String principalKey, String fallbackScene) {
        long start = System.currentTimeMillis();
        String traceId = newTraceId();
        String scene = normalizeScene(request.getScene(), fallbackScene);
        String resolvedPrincipal = resolvePrincipalKey(userId, principalKey);

        AgentRateLimitDecision decision = rateLimitService.checkLimit(scene, resolvedPrincipal);
        if (!decision.isAllowed()) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "AI_RATE_LIMITED");
        }

        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<ChatMessage> recentMessages = memoryService.getRecentMessages(conversationId, 8);
        AgentExecutionPlan plan = planningService.plan(request, recentMessages);
        List<AgentToolTrace> toolTrace = localLifeAgentTools.executePreferredTools(plan, request, userId, resolvedPrincipal);
        List<RetrievalHit> retrievalHits = localLifeRagService.retrieve(request.getMessage(), plan);
        String answer = buildAnswer(plan, toolTrace, retrievalHits);
        long latencyMs = System.currentTimeMillis() - start;

        memoryService.appendTurn(conversationId, request.getMessage(), answer);
        memoryService.saveConversationSummary(conversationId, summarizeConversation(plan, request.getMessage()));

        AgentAuditRecord record = new AgentAuditRecord();
        record.setTraceId(traceId);
        record.setUserId(userId);
        record.setPrincipalKey(resolvedPrincipal);
        record.setConversationId(conversationId);
        record.setIntent(plan.getIntent());
        record.setRetrievalCount(retrievalHits.size());
        record.setToolTrace(toolTrace);
        record.setLatencyMs(latencyMs);
        record.setScene(scene);
        record.setTimestampEpochMs(Instant.now().toEpochMilli());
        auditService.publish(record);

        AgentChatResponse response = new AgentChatResponse();
        response.setAnswer(answer);
        response.setConversationId(conversationId);
        response.setTraceId(traceId);
        response.setPlan(plan);
        response.setToolTrace(toolTrace);
        response.setRetrievalHits(retrievalHits);
        response.setLatencyMs(latencyMs);
        return response;
    }

    private void pushStreamEvents(SseEmitter emitter, AgentChatRequest request, Long userId, String principalKey) {
        try {
            AgentChatResponse response = doChat(request, userId, principalKey, "stream");

            AgentSseMetaEvent meta = new AgentSseMetaEvent();
            meta.setConversationId(response.getConversationId());
            meta.setTraceId(response.getTraceId());
            meta.setIntent(response.getPlan().getIntent());
            emitter.send(SseEmitter.event().name("meta").data(meta));

            for (String part : chunk(response.getAnswer(), 24)) {
                AgentSseChunkEvent chunk = new AgentSseChunkEvent();
                chunk.setContent(part);
                emitter.send(SseEmitter.event().name("chunk").data(chunk));
            }

            AgentSseDoneEvent done = new AgentSseDoneEvent();
            done.setConversationId(response.getConversationId());
            done.setTraceId(response.getTraceId());
            done.setLatencyMs(response.getLatencyMs());
            emitter.send(SseEmitter.event().name("done").data(done));
            emitter.complete();
        } catch (ResponseStatusException ex) {
            publishStreamError(emitter, "AI_RATE_LIMITED", ex.getReason());
        } catch (IOException ex) {
            log.error("Failed to push SSE response", ex);
            emitter.completeWithError(ex);
        } catch (RuntimeException ex) {
            log.error("SSE processing failed", ex);
            publishStreamError(emitter, "AI_SERVICE_UNAVAILABLE", ex.getMessage());
        }
    }

    private void publishStreamError(SseEmitter emitter, String code, String message) {
        AgentSseErrorEvent error = new AgentSseErrorEvent();
        error.setCode(code);
        error.setMessage(message);
        error.setTraceId(newTraceId());
        try {
            emitter.send(SseEmitter.event().name("error").data(error));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }

    private String normalizeScene(String scene, String fallback) {
        if (StringUtils.hasText(scene)) {
            return scene.trim().toLowerCase(Locale.ROOT);
        }
        return fallback;
    }

    private String resolvePrincipalKey(Long userId, String principalKey) {
        if (userId != null) {
            return "u:" + userId;
        }
        if (StringUtils.hasText(principalKey)) {
            return principalKey;
        }
        return "anonymous";
    }

    private String summarizeConversation(AgentExecutionPlan plan, String message) {
        return "intent=" + plan.getIntent() + "; message=" + message;
    }

    private String buildAnswer(AgentExecutionPlan plan, List<AgentToolTrace> toolTrace, List<RetrievalHit> retrievalHits) {
        List<String> parts = new ArrayList<>();
        parts.add("已完成意图规划：" + plan.getIntent() + "。");
        parts.add("工具调用次数：" + toolTrace.size() + "。");
        parts.add("知识命中条数：" + retrievalHits.size() + "。");
        parts.add("价格、优惠券、距离、营业状态等动态事实来自Tool/Service查询结果。");
        if (!retrievalHits.isEmpty()) {
            parts.add("平台规则与客服说明由RAG命中内容补充。");
        }
        return String.join("", parts);
    }

    private List<String> chunk(String answer, int maxChunkSize) {
        if (!StringUtils.hasText(answer)) {
            return List.of("");
        }
        List<String> chunks = new ArrayList<>();
        int from = 0;
        while (from < answer.length()) {
            int to = Math.min(answer.length(), from + maxChunkSize);
            chunks.add(answer.substring(from, to));
            from = to;
        }
        return chunks;
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
