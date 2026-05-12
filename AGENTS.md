# AGENTS.md - 新港社区 AI 协作路线手册

本文档是给 Codex、DeepSeek、Copilot、Claude Code 等 AI 开发执行者阅读的项目协作入口。每完成一轮开发、Review、合并或任务重新规划，产品经理 AI 必须更新本文档，让后续 AI 能快速理解项目当前状态、开发方向、分支使用方式和执行者职责。

---

## 1. 必读顺序

新接入本项目的 AI 必须按以下顺序阅读：

1. `AGENTS.md`：先理解当前项目状态、分支状态、任务方向和协作规则。
2. `新港社区项目开发文档.md`：理解完整需求、技术架构、人员分工、编码规范、接口、数据库、Redis、AI边界和Review标准。
3. `docs/superpowers/plans/2026-05-12-phase2-development.md`：理解当前第二阶段任务拆分、验收标准和每个执行者的职责。
4. 相关源码模块：只阅读自己负责范围内的代码，避免越权修改。

如果本文档与开发文档冲突，以 `新港社区项目开发文档.md` 为准；如果任务计划与产品经理最新口头指令冲突，以产品经理最新指令为准。

---

## 2. 项目定位

项目名称：新港社区  
项目全称：新港社区：AI 本地生活服务与智能客服平台

项目目标：

1. 构建本地生活后端基础能力，包括用户、商户、优惠券、秒杀、订单、Redis缓存、Redis GEO、Redis Stream和Redisson锁。
2. 构建 AI Agent 能力，包括结构化规划、Tool Calling、RAG、会话记忆、SSE流式响应、限流和审计。
3. 明确边界：动态事实必须来自 Tool / Service / DB / Redis；RAG 只提供平台规则、客服FAQ和推荐约束；LLM只负责理解、编排和表达。

禁止把以下内容写成当前已完成主实现：

```text
RocketMQ
LangChain4j
Sa-Token
Milvus
Elasticsearch
MCP
支付网关
完整订单支付闭环
超时关单完整闭环
```

---

## 3. 当前 Git 状态

当前主集成分支：

```text
develop
```

当前已合并到 `develop` 的主要内容：

```text
d502c3e docs(plan): add phase 2 multi-agent development plan
90fd8b9 chore(git): ignore local agent skill state
8796a87 merge: integrate business module skeleton
23bd985 merge: integrate ai agent module skeleton
847a2c0 chore(common): add shared result and exception baseline
c24187f chore(project): establish shared development baseline
```

当前 worktree 约定：

```text
E:\javacode\xingang community             develop，产品经理集成和Review
E:\javacode\xingang-community-deepseek    feature/deepseek-business-phase2，开发人员A/DeepSeek
E:\javacode\xingang-community-copilot     feature/copilot-ai-phase2，开发人员B/Copilot
```

旧分支状态：

```text
feature/deepseek-business      第一阶段业务骨架，已合并，不再继续开发
feature/copilot-ai             第一阶段AI骨架，已合并，不再继续开发
```

第二阶段分支状态：

```text
feature/deepseek-business-phase2   已提交但未合并，Review结论：NEEDS CHANGES
feature/copilot-ai-phase2          已提交但未合并，Review结论：NEEDS CHANGES
```

---

## 4. 已完成内容

### 4.1 公共工程基线

已在 `develop` 完成：

1. `pom.xml`：统一 Spring Boot、Redis、Redisson、MyBatis-Plus、MySQL、Actuator、Prometheus、Lombok、测试依赖。
2. `application.yml`：统一服务端口、MySQL、Redis、MyBatis-Plus、Actuator、AI RAG、限流、审计、地图和模型占位配置。
3. `XingangCommunityApplication.java`：统一启动类，包含 Mapper 扫描。
4. `.gitignore`：忽略 `target/`、`.vscode/`、`.agents/`、`.claude/skills/`、`skills-lock.json` 等本地工具状态。

### 4.2 公共基础模块

已在 `develop` 完成：

1. `Result`：统一非SSE HTTP响应结构。
2. `TraceContext`：统一traceId上下文。
3. `TraceIdFilter`：读取或生成 `X-Trace-Id`。
4. `ErrorCode`：统一错误码。
5. `BusinessException`：统一业务异常。
6. `GlobalExceptionHandler`：统一异常响应转换。

### 4.3 业务模块第一阶段

已合并到 `develop`：

1. 用户登录骨架。
2. 商户详情、分类、按分类查询、Redis缓存、Redis GEO附近查询骨架。
3. 优惠券按商户查询骨架。
4. 秒杀入口、Lua脚本、Redis库存Key、一人一单Key、Redis Stream消息骨架。
5. Redisson用户锁、订单事务服务骨架。

### 4.4 AI模块第一阶段

已合并到 `develop`：

1. `/ai/agent/chat`
2. `/ai/agent/chat/stream`
3. `/ai/agent/session`
4. `/ai/agent/knowledge/rebuild`
5. Planner结构化规划骨架。
6. Tool Calling骨架。
7. RAG知识检索骨架。
8. Redis会话记忆骨架。
9. Agent限流、审计、SSE事件结构。

---

## 5. 当前第二阶段 Review 结论

第二阶段两个执行者均已提交，且单分支编译通过：

```text
feature/deepseek-business-phase2
mvn -q -DskipTests compile 通过

feature/copilot-ai-phase2
mvn -q -DskipTests compile 通过
```

但当前结论是：

```text
NEEDS CHANGES
```

暂不合并 phase2，原因如下。

### 5.1 DeepSeek 分支问题

1. Redis Stream 消费组初始化可能在空 Stream 下失败。必须使用 `MKSTREAM` 或等价方式确保 `stream.orders` 和消费组真实存在。
2. pending-list 处理返回值不准确。当前处理计数不等于 ACK 成功数，需要 `processSingleMessage` 返回 boolean，只有 ACK 成功才计数。
3. 商户缓存锁释放已经使用 token，但 `GET` + `DELETE` 非原子。后续正式高并发优化应使用 Lua compare-and-delete 或 Redisson。
4. 秒杀库存初始化已做启动加载，但要明确它只在 Redis Key 不存在时写入，避免覆盖运行时库存。

### 5.2 Copilot 分支问题

1. Planner 抽取预算单位为“元”，数据库 `avg_price` 字段为“分”，推荐过滤直接比较导致错杀候选。必须统一单位。
2. `get_shop_detail` 和 `get_shop_coupons` 已能接收 `shopId`，但 `executePreferredTools` 中仍传 `null`，实际编排无法查到真实详情和优惠券。
3. 推荐工具返回候选后，需要将候选 `shopId` 级联给详情/优惠券工具，或在同一个推荐结果中明确包含优惠券摘要。
4. `resolveDiscount(payValue, actualValue)` 当前按 `payValue - actualValue` 计算，可能不符合业务语义，需要结合优惠券字段含义调整或命名为 `payValue` / `actualValue`。

---

## 6. 下一轮修复任务方向

### 6.1 给 DeepSeek 的下一轮修复范围

只修业务模块，不碰 AI 模块。

必须修复：

1. Redis Stream 消费组初始化：
   - 使用 `XGROUP CREATE stream.orders order-group 0 MKSTREAM` 等价逻辑。
   - 不允许把任意异常都当作“消费组已存在”。
   - 已存在时可以忽略；其他异常必须明确记录。

2. pending-list 成功计数：
   - 将 `processSingleMessage` 改成返回 `boolean`。
   - 只有事务创建成功并 ACK 成功时返回 `true`。
   - `handlePendingList` 只统计 `true`。

3. 明确秒杀库存初始化策略：
   - Redis Key 不存在时才初始化。
   - Redis Key 已存在时不覆盖。
   - 在最终回复说明这样做的原因。

可以暂不修：

1. 商户缓存锁释放的 Lua 原子删除。
2. 完整死信队列。
3. 管理后台接口。

### 6.2 给 Copilot 的下一轮修复范围

只修 AI 模块和必要 Service 接口适配，不碰秒杀/订单/Lua/事务消费者。

必须修复：

1. 预算单位：
   - 选择一种统一策略，并在代码中明确。
   - 推荐方案：Planner 仍抽取用户自然语言中的“元”，进入推荐过滤前转换为“分”。

2. Tool级联：
   - `recommend_shops_v2` / `recommend_nearby_shops` 返回候选后，能够拿候选 `shopId` 查询优惠券。
   - `executePreferredTools` 不应对 `get_shop_detail` / `get_shop_coupons` 永远传 `null`。
   - 如果用户没有指定明确 shopId，应基于推荐候选的 top shopId 查询。

3. 动态事实边界：
   - 价格、优惠券、距离、营业状态必须来自 Service / Tool 结果。
   - RAG 不能补动态事实。

4. 优惠券字段语义：
   - 避免把字段误命名为 discountAmount 却计算不清。
   - 如果无法确认真实优惠含义，保留 payValue、actualValue 或增加说明字段。

可以暂不修：

1. 真实大模型调用。
2. 百度地图地理编码。
3. 复杂推荐 scoreBreakdown。

---

## 7. 产品经理 AI 如何指挥两个执行者

### 7.1 每轮任务开始前

产品经理 AI 必须先和用户确认：

1. 本轮目标是补骨架、做闭环、修bug、写测试还是准备演示。
2. 哪些复杂功能本轮必须完整实现。
3. 哪些复杂功能只做骨架或预留。
4. 是否允许修改公共基线文件。
5. 是否允许新增依赖、配置、SQL、接口。

未经确认，不要直接派发大任务。

### 7.2 提示词必须要求执行者先反馈复杂实现方案

后续给 DeepSeek 或 Copilot 的提示词中必须包含：

```text
在正式编码前，请先输出复杂功能的具体实现方案，等待确认后再动手：
1. 你会修改哪些文件？
2. 关键流程如何实现？
3. 是否会影响另一个执行者负责的模块？
4. 是否会修改公共基线？
5. 哪些内容本轮完整实现，哪些只做骨架？
```

### 7.3 提示词必须要求执行者最终反馈实现细节

每个执行者完成后必须反馈：

```text
1. 修改文件清单。
2. 关键复杂功能如何实现。
3. 为什么选择这种实现方式。
4. 边界情况如何处理。
5. 哪些风险仍存在。
6. 运行了什么验证命令，结果如何。
7. 是否修改公共基线。
```

### 7.4 Review 后必须更新本文档

每次完成以下动作后，产品经理 AI 必须更新 `AGENTS.md`：

1. 一轮开发任务派发完成。
2. 两个执行者提交完成。
3. Code Review 完成。
4. 分支合并完成。
5. 项目方向或职责边界发生变化。
6. 出现新的阻断问题或重要技术决策。

更新内容至少包括：

1. 当前分支状态。
2. 已完成内容。
3. 未合并内容。
4. Review结论。
5. 下一步任务。
6. 给其他 AI 的注意事项。

---

## 8. 当前不允许做的事

任何 AI 不得在未获得产品经理确认前执行以下操作：

1. 合并 phase2 分支到 `develop`。
2. 删除 worktree。
3. 删除旧分支。
4. 引入新框架或新中间件。
5. 把后续增强项写成已完成。
6. 修改公共基线文件。
7. 同时让两个执行者改同一组文件。
8. 在未运行验证命令前声称完成。

---

## 9. 当前推荐下一步

下一步不是继续扩展新功能，而是先修复 phase2 Review 问题：

1. DeepSeek 修 Redis Stream 消费组初始化和 pending-list 成功计数。
2. Copilot 修预算单位和 Tool 级联真实 shopId 查询。
3. 产品经理 AI Review 两个修复分支。
4. 若通过，先合 DeepSeek，再合 Copilot。
5. 合并后在 `develop` 运行：

```bash
mvn -q -DskipTests compile
```

6. 更新 `AGENTS.md`，记录 phase2 完成情况。
