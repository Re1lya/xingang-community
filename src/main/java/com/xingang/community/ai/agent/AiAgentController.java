package com.xingang.community.ai.agent;

import com.xingang.community.ai.agent.dto.AgentChatRequest;
import com.xingang.community.ai.agent.dto.AgentChatResponse;
import com.xingang.community.ai.agent.dto.AgentSessionClearResponse;
import com.xingang.community.ai.agent.dto.KnowledgeRebuildResponse;
import com.xingang.community.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/ai/agent")
public class AiAgentController {

    private final AgentOrchestrationService orchestrationService;

    public AiAgentController(AgentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                          @RequestHeader(value = "X-Principal-Key", required = false) String principalKey) {
        return Result.ok(orchestrationService.chat(request, userId, principalKey));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AgentChatRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                 @RequestHeader(value = "X-Principal-Key", required = false) String principalKey) {
        return orchestrationService.streamChat(request, userId, principalKey);
    }

    @DeleteMapping("/session")
    public Result<AgentSessionClearResponse> clearSession(@RequestParam(value = "conversationId", required = false) String conversationId,
                                                          @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                          @RequestHeader(value = "X-Principal-Key", required = false) String principalKey) {
        return Result.ok(orchestrationService.clearSession(conversationId, userId, principalKey));
    }

    @PostMapping("/knowledge/rebuild")
    public Result<KnowledgeRebuildResponse> rebuildKnowledge(@RequestHeader(value = "X-Agent-Admin", defaultValue = "false") boolean admin,
                                                             @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                             @RequestHeader(value = "X-Principal-Key", required = false) String principalKey) {
        if (!admin) {
            throw new ResponseStatusException(FORBIDDEN, "knowledge rebuild requires admin privilege");
        }
        return Result.ok(orchestrationService.rebuildKnowledgeIndex(userId, principalKey));
    }
}
