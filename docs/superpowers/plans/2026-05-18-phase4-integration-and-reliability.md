# Phase 4 Integration and Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the next phase around frontend/backend AI streaming integration and business reliability hardening without expanding into unapproved frameworks.

**Architecture:** Copilot owns AI backend streaming and answer payload semantics. DeepSeek owns business reliability around Redis Stream DLQ idempotency and seckill operations. The user owns frontend implementation directly on `develop` using the frontend prompt in this plan, while keeping dynamic facts sourced from backend Tool/Service responses.

**Tech Stack:** Spring Boot, Spring MVC `SseEmitter`, Redis Stream, Redis Lua, Redisson, MyBatis-Plus, React, Vite, TypeScript, TanStack Query.

---

## Branches and Ownership

```text
E:\javacode\xingang-community-copilot    feature/copilot-ai-phase4
E:\javacode\xingang-community-deepseek   feature/deepseek-business-phase4
E:\javacode\xingang community            develop/main; user may implement frontend directly on develop
```

Rules:

- Both executor branches must start from current `main` / `develop` at `5a071f1`.
- Do not continue development on `feature/copilot-ai-phase3` or `feature/deepseek-business-phase3`.
- Copilot must not modify `src/main/java/com/xingang/community/order/**`, `src/main/java/com/xingang/community/voucher/**`, or Redis Stream business internals.
- DeepSeek must not modify `src/main/java/com/xingang/community/ai/**` or `frontend/**`.
- Frontend work must keep API contracts aligned with backend event names and response fields.
- Every executor must run `mvn -q -DskipTests compile` before reporting completion.
- Frontend implementer must run `npm run build` inside `frontend/` before reporting completion.

---

## Phase 4 Scope

This phase has two goals and one direct frontend task:

1. AI streaming demo experience:
   - Add SSE `stage` events so the UI can show Agent progress.
   - Preserve existing `meta`, `chunk`, `done`, and `error` events.
   - Keep final answers grounded in `ToolExecutionSnapshot`, `AgentToolTrace`, and RAG hits.

2. Business reliability option C:
   - Add DLQ idempotency around `originalMessageId`.
   - Add read-only DLQ observability.
   - Prepare guarded retry/reset skeletons only where explicitly scoped.

3. Frontend integration:
   - Show stage progress, tool calls, `traceId`, streaming text, and clear error state on the AI chat page.

---

## Task A: Copilot AI Streaming Backend

**Owner:** Copilot / 开发人员B

**Branch:** `feature/copilot-ai-phase4`

**Allowed files:**

- `src/main/java/com/xingang/community/ai/agent/**`
- `src/main/java/com/xingang/community/ai/agent/dto/sse/**`
- `src/main/java/com/xingang/community/ai/tool/model/**`
- `docs/superpowers/phase4-ai-streaming-examples.md`

**Forbidden files:**

- `src/main/java/com/xingang/community/order/**`
- `src/main/java/com/xingang/community/voucher/**`
- `src/main/java/com/xingang/community/shop/**` internals
- `src/main/resources/db/schema.sql`
- `pom.xml`

### Requirements

- Add a new SSE event type named `stage`.
- Stage event payload must be structured and stable:

```json
{
  "stage": "planning",
  "label": "正在理解需求",
  "status": "running",
  "traceId": "agent-...",
  "detail": "optional short detail"
}
```

- Supported stage names for this round:

```text
planning
tool_call
rag
answering
```

- Suggested lifecycle:

```text
meta
stage(planning,running)
stage(planning,done)
stage(tool_call,running)
stage(tool_call,done)
stage(rag,running)
stage(rag,done)
stage(answering,running)
chunk...
stage(answering,done)
done
```

- `error` events must still include `traceId`.
- Do not duplicate business calls just to emit stages. If current `doChat` is too coarse, refactor minimally so streaming can emit stages while reusing the same planning/tool/RAG/answer logic.
- The final answer must continue to use `AnswerComposer`.
- Tool facts must still come from `ToolExecutionSnapshot`.
- RAG must still only provide static rule/support context.

### Suggested Implementation Steps

- [ ] Create `AgentSseStageEvent` in `src/main/java/com/xingang/community/ai/agent/dto/sse/AgentSseStageEvent.java` with fields `stage`, `label`, `status`, `traceId`, `detail`.
- [ ] Add a private helper in `AgentOrchestrationServiceImpl`:

```java
private void sendStage(SseEmitter emitter, String traceId, String stage, String label, String status, String detail) throws IOException {
    AgentSseStageEvent event = new AgentSseStageEvent();
    event.setTraceId(traceId);
    event.setStage(stage);
    event.setLabel(label);
    event.setStatus(status);
    event.setDetail(detail);
    emitter.send(SseEmitter.event().name("stage").data(event));
}
```

- [ ] Refactor `pushStreamEvents` so it emits stage events around planning, tool execution, RAG retrieval, and answer chunking.
- [ ] Keep synchronous `/ai/agent/chat` behavior unchanged except for shared private helper extraction if needed.
- [ ] Add `docs/superpowers/phase4-ai-streaming-examples.md` with one normal chat curl and one SSE curl showing `stage`, `chunk`, and `done`.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Commit with:

```bash
git commit -m "feat(agent): add sse stage events for ai workflow"
```

### Completion Report Must Include

- Changed files.
- Exact SSE event order.
- Whether `/ai/agent/chat` changed.
- How traceId is preserved across `meta`, `stage`, `chunk`, `done`, and `error`.
- Verification command and result.

---

## Task B: DeepSeek Business Reliability Option C

**Owner:** DeepSeek / 开发人员A

**Branch:** `feature/deepseek-business-phase4`

**Allowed files:**

- `src/main/java/com/xingang/community/order/**`
- `src/main/java/com/xingang/community/common/constant/RedisConstants.java`
- `src/main/resources/db/schema.sql`
- `docs/superpowers/phase4-business-reliability-examples.md`

**Forbidden files:**

- `src/main/java/com/xingang/community/ai/**`
- `frontend/**`
- `pom.xml`
- AI docs or prompt examples outside the business reliability examples file.

### Requirements

- Implement DLQ idempotency using `originalMessageId`.
- Add a Redis Set key constant for DLQ routed message IDs:

```text
stream.orders.dlq:routed
```

- When a pending message exceeds `MAX_DELIVERY_COUNT`:
  - If `originalMessageId` is not in the routed set, write it to `stream.orders.dlq`, add it to the routed set, then `XACK` the original message.
  - If `originalMessageId` is already in the routed set, do not write a duplicate DLQ record; only retry `XACK`.
- The idempotent check and DLQ write should be made atomic where practical. Preferred implementation is a Redis Lua script for `SISMEMBER + XADD + SADD`, with Java still performing `XACK` after DLQ write/known-routed status.
- Do not drop the original pending message unless `XACK` returns `ack > 0`.
- Add read-only admin endpoint:

```text
GET /admin/seckill/dlq
```

- The endpoint must require the existing `X-Admin-Token` check.
- The endpoint should return up to 50 DLQ messages with `originalMessageId`, `orderId`, `userId`, `voucherId`, `failureReason`, `deliveryCount`, and `movedAt`.
- Do not implement destructive retry or ignore endpoints in this round unless product manager explicitly approves later.
- Do not implement stock reset in this round unless product manager explicitly approves later.

### Suggested Implementation Steps

- [ ] Add `STREAM_ORDERS_DLQ_ROUTED_KEY` to `RedisConstants`.
- [ ] Create `src/main/resources/lua/route_to_dlq.lua` if using Lua:

```lua
local routedKey = KEYS[1]
local dlqKey = KEYS[2]
local originalMessageId = ARGV[1]

if redis.call('SISMEMBER', routedKey, originalMessageId) == 1 then
    return 2
end

redis.call(
    'XADD',
    dlqKey,
    '*',
    'orderId', ARGV[2],
    'userId', ARGV[3],
    'voucherId', ARGV[4],
    'originalMessageId', originalMessageId,
    'failureReason', ARGV[5],
    'deliveryCount', ARGV[6],
    'movedAt', ARGV[7]
)
redis.call('SADD', routedKey, originalMessageId)
return 1
```

- [ ] Interpret Lua return values in Java:

```text
1 = new DLQ record written
2 = already routed, skip duplicate XADD
```

- [ ] Only count routed success after `XACK > 0`.
- [ ] Add DTO for DLQ message detail if existing pending DTO is not appropriate.
- [ ] Add read-only `getDlqMessages` method to `SeckillObservabilityService`.
- [ ] Add `GET /admin/seckill/dlq` to `SeckillAdminController`.
- [ ] Update `schema.sql` Redis Stream notes to describe `stream.orders.dlq:routed`.
- [ ] Add `docs/superpowers/phase4-business-reliability-examples.md` with admin curl examples using `X-Admin-Token`.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Commit with:

```bash
git commit -m "feat(business): add dlq idempotency and observability"
```

### Completion Report Must Include

- Changed files.
- Exact DLQ idempotency flow.
- What happens when XADD succeeds but XACK fails.
- What happens when `originalMessageId` was already routed.
- Why retry/ignore/stock reset are not implemented in this round.
- Verification command and result.

---

## Task C: Frontend Integration Prompt for User

**Owner:** User, directly on `develop`

**Allowed files:**

- `frontend/src/types/ai.ts`
- `frontend/src/services/aiApi.ts`
- `frontend/src/hooks/useSseChat.ts`
- `frontend/src/pages/AiChatPage.tsx`
- `frontend/src/components/ai/**`
- `frontend/src/styles/**`

**Forbidden files:**

- Backend Java files.
- `pom.xml`.
- Business SQL or Redis constants.

### Copyable Prompt

```text
你当前工作目录：
E:\javacode\xingang community

你当前分支：
develop

请先执行：
1. git status --short --branch
2. 阅读 AGENTS.md
3. 阅读 新港社区前端开发文档.md
4. 阅读 PRODUCT.md
5. 阅读 docs/superpowers/plans/2026-05-18-phase4-integration-and-reliability.md

本轮前端任务目标：
基于后端即将新增的 SSE stage 事件，完善 AI 对话页的前后端联调体验，让用户能看到 Agent 的工作阶段、traceId、工具调用状态、流式回答和错误信息。

允许修改范围：
1. frontend/src/types/ai.ts
2. frontend/src/services/aiApi.ts
3. frontend/src/hooks/useSseChat.ts
4. frontend/src/pages/AiChatPage.tsx
5. frontend/src/components/ai/**
6. frontend/src/styles/**

禁止修改范围：
1. src/main/java/**
2. src/main/resources/**
3. pom.xml
4. 后端业务逻辑、AI编排逻辑、数据库脚本

在正式编码前，请先输出具体实现方案，等待确认后再动手：
1. 你会修改哪些前端文件？
2. SseChatEvent 类型如何兼容新增 stage 事件？
3. AiChatPage 如何展示 planning/tool_call/rag/answering 阶段？
4. traceId 如何展示？
5. 错误事件如何展示？
6. 如果后端还没有 stage 事件，前端如何兼容旧 meta/chunk/done/error？
7. 是否会影响普通问答模式？

编码要求：
1. 严格遵循 AGENTS.md、PRODUCT.md 和前端开发文档。
2. 不得在前端伪造价格、库存、优惠券、评分、距离、营业状态等动态事实。
3. stage 只能展示流程状态，不得当成业务事实。
4. 普通问答和流式问答都要保留。
5. 流式模式下展示：
   - 当前阶段
   - 阶段历史
   - traceId
   - 增量回答内容
   - 错误 message 和 traceId
6. UI 不要做营销落地页，保持工具型、清晰、可演示。
7. 所有文本必须在移动端和桌面端不溢出。

建议实现：
1. 在 frontend/src/types/ai.ts 增加：
   - AgentStageName = "planning" | "tool_call" | "rag" | "answering" | string
   - AgentStageStatus = "running" | "done" | "skipped" | "error" | string
   - AgentStageEventData
   - SseChatEvent 增加 { type: "stage"; data: AgentStageEventData }
2. 在 AiChatPage.tsx 中增加 stageEvents 状态数组。
3. handleStreamEvent 收到 stage 时更新 stageEvents。
4. meta/done 继续更新 conversationId 和 traceId。
5. chunk 继续追加到最后一条 assistant 消息。
6. error 显示错误内容并保留 traceId。
7. 增加一个轻量的 AgentProgressPanel 组件，展示阶段列表和当前状态。

完成后请自行检查：
1. npm run build
2. 检查普通问答仍可用。
3. 检查流式问答在没有 stage 事件时仍能展示 chunk/done。
4. 检查流式问答在收到 stage 事件时能展示阶段进度。
5. 检查没有修改后端文件。

完成后请提交：
git commit -m "feat(frontend): show ai streaming workflow stages"

最终反馈必须包含：
1. 修改文件清单。
2. SSE stage 前端类型如何设计。
3. AI 对话页如何展示阶段、traceId、回答和错误。
4. 如何兼容旧 SSE 事件。
5. 是否修改后端。
6. 运行了什么验证命令，结果如何。
```

---

## Product Manager Review Checklist

- [ ] Confirm `feature/copilot-ai-phase4` is clean and based on current `main` / `develop`.
- [ ] Review Copilot changed files against allowed AI/backend scope.
- [ ] Verify `stage` event payload fields and event order.
- [ ] Verify sync `/ai/agent/chat` still works.
- [ ] Run `mvn -q -DskipTests compile` for Copilot branch.
- [ ] Confirm `feature/deepseek-business-phase4` is clean and based on current `main` / `develop`.
- [ ] Review DeepSeek changed files against allowed business scope.
- [ ] Verify DLQ duplicate prevention uses `originalMessageId`.
- [ ] Verify `XACK > 0` remains required before counting success.
- [ ] Verify admin DLQ endpoint uses `X-Admin-Token`.
- [ ] Run `mvn -q -DskipTests compile` for DeepSeek branch.
- [ ] Review frontend diff on `develop` if user completes Task C before backend merge.
- [ ] Run `npm run build` inside `frontend/`.
- [ ] Merge only after review approval.
- [ ] Update `AGENTS.md` after review/merge.
