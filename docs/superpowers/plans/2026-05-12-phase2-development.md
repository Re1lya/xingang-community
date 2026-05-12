# Phase 2 Development Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the merged skeleton into a more usable development baseline by filling the business data path and wiring AI tools to real business services.

**Architecture:** The business track owns user/shop/voucher/order persistence and Redis-backed seckill behavior. The AI track owns Agent orchestration and may consume business capabilities only through Service interfaces. Shared files such as `pom.xml`, `application.yml`, `Result`, `TraceContext`, exception handling, and `.gitignore` are product-manager-owned public baseline files.

**Tech Stack:** Java 17 compile baseline, Spring Boot 3.5.x, MyBatis-Plus, MySQL, Redis, Redisson, Redis Stream, Lua, SSE.

---

## Global Rules

- Always start from the current `develop` branch.
- DeepSeek works only in `E:\javacode\xingang-community-deepseek` on `feature/deepseek-business-phase2`.
- Copilot works only in `E:\javacode\xingang-community-copilot` on `feature/copilot-ai-phase2`.
- Before coding, run `git merge develop`.
- Keep `develop` versions of public baseline files during conflicts.
- Do not commit `.agents/`, `.claude/skills/`, `skills-lock.json`, `.vscode/`, or `target/`.
- Before claiming completion, run `mvn -q -DskipTests compile` and report the exit result.

---

## Task A: Business Module Phase 2

**Owner:** DeepSeek / 开发人员A

**Files:**
- Create: `src/main/resources/db/schema.sql`
- Create: `src/main/resources/db/seed.sql`
- Modify: `src/main/java/com/xingang/community/common/constant/RedisConstants.java`
- Modify: `src/main/java/com/xingang/community/shop/service/impl/ShopServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/voucher/service/impl/VoucherServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/order/service/impl/VoucherOrderServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/order/service/impl/VoucherOrderTransactionServiceImpl.java`

### Requirements

- [ ] Add SQL schema for current skeleton entities: `user`, `shop`, `shop_type`, `voucher`, `seckill_voucher`, `voucher_order`.
- [ ] Add small seed data sufficient for local manual verification.
- [ ] Add a service method or startup-safe utility path for initializing Redis seckill stock from `seckill_voucher.stock`.
- [ ] Start the Redis Stream consumer from a managed background executor, not an uncontrolled raw thread.
- [ ] Implement basic pending-list processing for `stream.orders`.
- [ ] Keep one-user-one-voucher protection in both Redis Lua and MySQL transaction logic.
- [ ] Add cache invalidation method for shop updates if an update endpoint exists; if no update endpoint exists, document the service method and keep API scope unchanged.
- [ ] Keep `Result`, exception handling, `pom.xml`, `application.yml`, and startup class from `develop`.

### Verification

- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Run `git status --short` and confirm only intended source files are tracked.
- [ ] Report which features are complete and which are still skeleton.

### Commit

```bash
git add src/main/resources/db src/main/java/com/xingang/community
git commit -m "feat(business): add database scripts and seckill stream processing"
```

---

## Task B: AI Module Phase 2

**Owner:** Copilot / 开发人员B

**Files:**
- Modify: `src/main/java/com/xingang/community/ai/tool/LocalLifeAgentToolsImpl.java`
- Modify: `src/main/java/com/xingang/community/ai/recommend/ShopRecommendationServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/ai/agent/AgentOrchestrationServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/ai/audit/AgentAuditServiceImpl.java`
- Modify: `src/main/java/com/xingang/community/ai/ratelimit/AgentRateLimitServiceImpl.java`
- Modify as needed: `src/main/java/com/xingang/community/shop/service/ShopService.java`
- Modify as needed: `src/main/java/com/xingang/community/voucher/service/VoucherService.java`

### Requirements

- [ ] Wire `LocalLifeAgentToolsImpl` to business Service interfaces instead of returning all-empty skeleton facts.
- [ ] `search_shops` must call shop service query capabilities and return structured `ShopCandidate` facts.
- [ ] `get_shop_detail` must call shop service by shopId and return structured `ShopDetailFact`.
- [ ] `get_shop_coupons` must call voucher service by shopId and return structured `CouponFact`.
- [ ] `recommend_shops`, `recommend_shops_v2`, and `recommend_nearby_shops` must use business service candidates and keep dynamic facts from Tool results only.
- [ ] Keep RAG for static rules only; do not use RAG to invent price, stock, distance, coupons, or business status.
- [ ] Read Agent audit stream key from config if practical; otherwise preserve documented key `hmdp:agent:audit`.
- [ ] Keep SSE events as `meta`, `chunk`, `done`, `error`.
- [ ] Do not modify order, seckill, Lua, or transaction internals.

### Verification

- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Run `git status --short` and confirm only intended AI/service adapter files are tracked.
- [ ] Report each Tool and whether it is wired to real service data or still skeleton.

### Commit

```bash
git add src/main/java/com/xingang/community/ai src/main/java/com/xingang/community/shop/service src/main/java/com/xingang/community/voucher/service
git commit -m "feat(agent): wire local life tools to business services"
```

---

## Product Manager Review Gates

- [ ] Review branch diffs against `develop`.
- [ ] Verify DeepSeek does not modify AI implementation internals.
- [ ] Verify Copilot does not modify seckill/order internals.
- [ ] Run `mvn -q -DskipTests compile` after each merge.
- [ ] Resolve shared interface changes in `develop` if both branches need the same Service method.
