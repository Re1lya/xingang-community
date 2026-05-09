package com.xingang.community.ai.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgentAuditServiceImpl implements AgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditServiceImpl.class);
    private static final String AUDIT_STREAM_KEY = "hmdp:agent:audit";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AgentAuditServiceImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AgentAuditRecord record) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("traceId", safe(record.getTraceId()));
        payload.put("userId", record.getUserId() == null ? "" : String.valueOf(record.getUserId()));
        payload.put("principalKey", safe(record.getPrincipalKey()));
        payload.put("conversationId", safe(record.getConversationId()));
        payload.put("intent", safe(record.getIntent()));
        payload.put("retrievalCount", String.valueOf(record.getRetrievalCount() == null ? 0 : record.getRetrievalCount()));
        payload.put("toolTrace", serializeToolTrace(record));
        payload.put("latencyMs", String.valueOf(record.getLatencyMs() == null ? 0L : record.getLatencyMs()));
        payload.put("scene", safe(record.getScene()));
        payload.put("timestampEpochMs", String.valueOf(record.getTimestampEpochMs() == null ? 0L : record.getTimestampEpochMs()));

        try {
            RecordId recordId = stringRedisTemplate.opsForStream()
                    .add(StreamRecords.string(payload).withStreamKey(AUDIT_STREAM_KEY));
            log.debug("agent audit published: stream={}, id={}", AUDIT_STREAM_KEY, recordId);
        } catch (RuntimeException ex) {
            log.error("agent audit publish failed, traceId={}", record.getTraceId(), ex);
        }
    }

    private String serializeToolTrace(AgentAuditRecord record) {
        try {
            return objectMapper.writeValueAsString(record.getToolTrace());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize tool trace", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
