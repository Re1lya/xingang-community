# Phase 3 Development Skeleton

> **For agentic workers:** Read `AGENTS.md` first, then `新港社区项目开发文档.md`, then this plan. Before coding, each executor must first output an implementation approach and wait for confirmation.

**Goal:** Build the third-stage development skeleton after phase2 integration. This stage focuses on production-readiness and demo usability without introducing unapproved frameworks.

**Current baseline:** `develop` includes phase2 business and AI integrations. `mvn -q -DskipTests compile` passes on `develop`.

**Branches:**

```text
E:\javacode\xingang-community-deepseek   feature/deepseek-business-phase3
E:\javacode\xingang-community-copilot    feature/copilot-ai-phase3
```

---

## Global Rules

- Start from current `develop`.
- Do not continue development on `feature/deepseek-business-phase2` or `feature/copilot-ai-phase2`.
- Do not delete old phase2 branches or worktrees unless the product-manager AI explicitly confirms with the user.
- Do not modify public baseline files unless the task explicitly allows it.
- Do not introduce RocketMQ, LangChain4j, Sa-Token, Milvus, Elasticsearch, MCP, or payment gateways.
- Run `mvn -q -DskipTests compile` before reporting completion.
- Final response from each executor must include changed files, implementation details, verification command, result, risks, and whether public baseline was modified.

---

## Task A: Business Reliability Skeleton

**Owner:** DeepSeek / 开发人员A

**Branch:** `feature/deepseek-business-phase3`

**Allowed scope:**

- `src/main/java/com/xingang/community/order/**`
- `src/main/java/com/xingang/community/voucher/**`
- `src/main/java/com/xingang/community/shop/**`
- `src/main/resources/db/**`
- Business-only DTO/VO/controller additions if required by the confirmed task

**Do not modify:**

- `src/main/java/com/xingang/community/ai/**`
- `pom.xml`
- `application.yml`
- Public exception/result/trace baseline unless explicitly confirmed

### Candidate Work Items

Pick only the items confirmed for the current round:

- [ ] Redis Stream dead-letter queue skeleton:
  - Define dead-letter stream name and retry threshold.
  - Do not silently drop failed order messages.
  - Keep original message fields and failure reason.
- [ ] Pending-list observability skeleton:
  - Expose pending count through a service method or admin-style endpoint if confirmed.
  - Keep read-only observability separate from mutation.
- [ ] Manual seckill stock reset skeleton:
  - Define request/response DTOs if endpoint is confirmed.
  - Reset Redis stock only through explicit management action.
  - Do not overwrite runtime stock on service startup.
- [ ] Business verification scenarios:
  - Duplicate order.
  - Stock sold out.
  - Stream consumer ACK success/failure.
  - Pending-list retry.

### Required Pre-Coding Response

Before coding, DeepSeek must answer:

```text
1. Which item(s) from Task A will be implemented in this round?
2. Which files will be modified?
3. How will retry/dead-letter or reset logic avoid corrupting order state?
4. Will any public baseline file be modified?
5. Which parts are complete implementation and which are skeleton only?
```

### Acceptance Criteria

- `mvn -q -DskipTests compile` passes.
- No AI module files changed.
- Existing seckill order path still uses Lua + Redis Stream + Redisson + transaction service.
- Runtime stock is not overwritten on restart.
- New management or observability behavior is clearly separated from user-facing seckill APIs.

---

## Task B: AI Answer Experience Skeleton

**Owner:** Copilot / 开发人员B

**Branch:** `feature/copilot-ai-phase3`

**Allowed scope:**

- `src/main/java/com/xingang/community/ai/**`
- `src/main/resources/knowledge/**`
- AI-only DTO/model additions if required by the confirmed task

**Do not modify:**

- `src/main/java/com/xingang/community/order/**`
- `src/main/java/com/xingang/community/voucher/**`
- `src/main/java/com/xingang/community/shop/**` internals
- `pom.xml`
- `application.yml`
- Public baseline files unless explicitly confirmed

### Candidate Work Items

Pick only the items confirmed for the current round:

- [ ] Tool fact answer composer skeleton:
  - Convert tool traces and tool facts into useful recommendation text.
  - Do not invent price, coupon, stock, distance, address, or business status.
  - If facts are unavailable, explain the missing condition.
- [ ] Context reference skeleton:
  - Support simple references such as "第一家", "这家", "刚才那个" using conversation memory or last tool result if available.
  - If context is insufficient, ask for clarification instead of guessing.
- [ ] Demo prompt set:
  - Add 10 recommendation prompts.
  - Add 10 support/RAG prompts.
  - Keep prompts in docs or resources, not hardcoded into services.
- [ ] Interface example skeleton:
  - Add curl examples for chat, stream, knowledge rebuild, and session clear.
  - Mark environment-dependent fields clearly.

### Required Pre-Coding Response

Before coding, Copilot must answer:

```text
1. Which item(s) from Task B will be implemented in this round?
2. Which files will be modified?
3. How will dynamic facts be carried from Tool/Service into final answer text?
4. How will missing facts and ambiguous references be handled?
5. Will any public baseline file be modified?
6. Which parts are complete implementation and which are skeleton only?
```

### Acceptance Criteria

- `mvn -q -DskipTests compile` passes.
- No order/seckill/Lua/transaction internals changed.
- Final answer text does not fabricate dynamic facts.
- RAG remains limited to rules, FAQ, and static guidance.
- Any demo data or prompt examples are clearly marked as examples.

---

## Product Manager AI Review Checklist

After both executors submit:

- [ ] Confirm both branches are clean.
- [ ] Confirm each branch is based on current `develop`.
- [ ] Review changed files against allowed scope.
- [ ] Run `mvn -q -DskipTests compile` in each worktree.
- [ ] Review for dynamic fact fabrication, Redis/transaction safety, and baseline drift.
- [ ] Update `AGENTS.md` after review.
- [ ] Merge only after approval.

