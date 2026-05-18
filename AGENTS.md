# AGENTS.md - 新港社区 AI 协作路线手册

本文档是给 Codex、DeepSeek、Copilot、Claude Code 等 AI 开发执行者阅读的项目协作入口。每完成一轮开发、Review、合并或任务重新规划，产品经理 AI 必须更新本文档，让后续 AI 能快速理解项目当前状态、开发方向、分支使用方式和执行者职责。

---

## 快速接手摘要：当前状态、固定流程和提示词标准

本节是给新接手的产品经理 AI 第一时间阅读的速览。读完本节后，再继续阅读下方完整角色要求、开发文档和阶段计划。

### A. 当前项目状态

截至 2026-05-18，项目处于第三阶段后端可靠性与 AI 回答体验合并完成阶段：

1. `develop` 已完成第二阶段合并、第三阶段前端骨架与 Review 修复，并已合并第三阶段 DeepSeek / Copilot 后端增强。当前 `develop` 最新集成提交为：

```text
b860b27 merge: integrate ai phase3 answer composer
```

2. 本次 Review 与合并状态：
   - `feature/deepseek-business-phase3` 已完成复审修复，最终提交为 `a9ec997 fix(business): harden seckill dlq ack and admin guard`。
   - `feature/copilot-ai-phase3` 已完成复审修复，最终提交为 `9831da0 fix(agent): distinguish skipped tools from empty facts`。
   - DeepSeek 分支已合并到 `develop`：`5e23e05 merge: integrate business phase3 reliability`。
   - Copilot 分支已合并到 `develop`：`b860b27 merge: integrate ai phase3 answer composer`。
   - 合并后在 `develop` 执行 `mvn -q -DskipTests compile` 通过。
   - 下一步需要完成本次 `AGENTS.md` 状态记录提交，并按用户要求同步到 `main`。

3. 第二阶段已合并内容：
   - DeepSeek 业务分支已合并：`eec3638 merge: integrate business phase2`
   - Copilot AI 分支已合并：`9260865 merge: integrate ai phase2`
   - 产品经理 AI 已完成集成修复：`d3e5fb2 chore(integration): finalize phase2 merge`

4. 当前工作区与分支：

```text
E:\javacode\xingang community             develop，当前集成分支，已合并 phase3 后端增强
E:\javacode\xingang-community-deepseek    feature/deepseek-business-phase3，已合并，后续不继续在该任务上新增范围
E:\javacode\xingang-community-copilot     feature/copilot-ai-phase3，已合并，后续不继续在该任务上新增范围
```

5. 第三阶段计划文档：

```text
docs/superpowers/plans/2026-05-12-phase3-development.md
```

6. 前端开发规划文档：

```text
新港社区前端开发文档.md
```

说明：当前已确认并创建 `frontend/` 第一轮骨架，允许新增 React/Vite 前端依赖和构建脚本。Review 修复已要求首页改走真实 `/shop/of/type`、评分不再除以 10、分类暂用文档演示常量、AI 对话支持普通/流式切换、SSE fetch 复用登录态请求头、秒杀失败展示 `message` 和 `traceId`。前端开发应继续以 `新港社区前端开发文档.md` 为基准，不得伪造动态事实。

7. 前端产品上下文：

```text
PRODUCT.md
```

说明：`PRODUCT.md` 是 `$impeccable` 使用的前端产品上下文文件，记录产品定位、用户、设计原则、反例和可访问性边界。后续改 UI 或新增前端页面时应保留并先读取。

8. 当前验证状态：

```text
develop                         mvn -q -DskipTests compile 通过（合并 DeepSeek 与 Copilot phase3 后验证）
feature/deepseek-business-phase3 mvn -q -DskipTests compile 通过（复审修复后验证）
feature/copilot-ai-phase3        mvn -q -DskipTests compile 通过（复审修复后验证）
```

9. 当前重要边界：
   - 不再继续使用 `feature/deepseek-business-phase2` 和 `feature/copilot-ai-phase2` 开发。
   - 不得删除旧分支或 worktree，除非用户明确确认。
   - 第三阶段两个执行者分支已合并到 `develop`，后续新任务应另开新分支或让执行者先同步最新 `develop`。
   - DeepSeek 已新增秒杀 pending-list / 库存可观测接口、DLQ 路由、ACK 结果校验和 `X-Admin-Token` 管理接口保护；`admin.token` 由 `${ADMIN_TOKEN:}` 注入，未配置时拒绝访问。
   - Copilot 已新增 `ToolExecutionSnapshot` 与 `AnswerComposer`，最终回答可基于真实 Tool 事实组织内容，并区分“未执行 Tool”和“执行后为空”。
   - 当前仍未实现 DLQ originalMessageId Redis Set 幂等去重、完整后台权限体系、完整 Agent 评估集和更自然的最终回答模板，这些属于后续增强。

### B. 产品经理 AI 固定工作流程

接手本项目的产品经理 AI 必须按以下流程工作：

1. 先读文档：
   - `AGENTS.md`
   - `新港社区项目开发文档.md`
   - `docs/superpowers/plans/2026-05-12-phase3-development.md`
   - 如涉及前端、React、页面、UI或SSE交互，还必须阅读 `新港社区前端开发文档.md`

2. 先和用户确认本轮目标，不要直接派发大任务。必须确认：
   - 本轮是做演示体验、测试补齐、真实接口闭环、自然语言回答优化，还是业务可靠性增强。
   - 哪些功能必须完整实现。
   - 哪些功能只做骨架。
   - 是否允许修改公共基线文件。
   - 是否允许新增依赖、配置、SQL或接口。

3. 给 DeepSeek / Copilot 派发任务时，必须使用本文档的提示词模板，不允许只说“你去修一下”。

4. 执行者编码前必须先反馈实现方案，产品经理 AI 审阅方案后再允许其编码。

5. 执行者完成后，产品经理 AI 必须：
   - 查看分支状态。
   - 查看最新提交。
   - 查看 diff。
   - 对照开发文档和阶段计划 Review。
   - 运行 `mvn -q -DskipTests compile`。
   - 给出 `APPROVED` / `NEEDS CHANGES` / `BLOCKED` 结论。

6. 每完成一轮任务、Review或合并，必须更新 `AGENTS.md`。

### C. 给执行者的标准提示词模板

后续所有给 DeepSeek 和 Copilot 的任务提示词，都必须按下面结构编写。可以根据任务内容增删细节，但不能删除“编码前先反馈方案”和“完成后反馈细节”两部分。

```text
你当前工作目录：
<填写 worktree 路径>

你当前分支：
<填写 feature 分支名>

请先执行：
1. git status --short --branch
2. git merge develop
3. 阅读 AGENTS.md
4. 阅读 新港社区项目开发文档.md
5. 阅读 docs/superpowers/plans/<当前阶段计划文件>

本轮任务目标：
<明确本轮要做什么，不要写泛泛目标>

允许修改范围：
<列出允许修改的目录和文件>

禁止修改范围：
<列出不得触碰的模块、公共基线、另一个执行者负责范围>

在正式编码前，请先输出复杂功能的具体实现方案，等待确认后再动手：
1. 你会修改哪些文件？
2. 关键流程如何实现？
3. 是否会影响另一个执行者负责的模块？
4. 是否会修改公共基线？
5. 哪些内容本轮完整实现，哪些只做骨架？
6. 有哪些边界情况、异常情况和回滚风险？

编码要求：
1. 严格遵循 AGENTS.md 和开发文档。
2. 不得引入未确认的新框架或新中间件。
3. 不得越权修改其他模块。
4. 动态事实必须来自 Service / DB / Redis / Tool，不得由 AI 编造。
5. 复杂逻辑必须有清晰注释，但不要堆无意义注释。

完成后请运行：
mvn -q -DskipTests compile

完成后请提交：
git commit -m "<填写规范提交信息>"

最终反馈必须包含：
1. 修改文件清单。
2. 关键复杂功能如何实现。
3. 为什么选择这种实现方式。
4. 边界情况如何处理。
5. 哪些风险仍存在。
6. 运行了什么验证命令，结果如何。
7. 是否修改公共基线。
```

### D. 已完成任务示例：第二阶段 Review、修复、合并

以下示例是本项目已经完成的一轮标准协作流程，后续 AI 必须照此节奏工作。

1. 阶段目标：
   - DeepSeek 负责业务侧第二阶段：数据库脚本、秒杀 Redis Stream 消费者、pending-list 恢复、秒杀库存初始化。
   - Copilot 负责 AI 侧第二阶段：AI Tool 接入业务 Service、推荐预算单位统一、推荐结果 shopId 级联到详情和优惠券。

2. 产品经理 AI 第一次 Review 发现问题：
   - DeepSeek：`stream.orders` 消费组初始化没有 `MKSTREAM`，空 Stream 首次启动可能失败；pending-list 处理计数没有区分 ACK 是否成功。
   - Copilot：Planner 抽取预算为“元”，数据库 `avg_price` 为“分”，直接比较会错杀候选；`get_shop_detail` 和 `get_shop_coupons` 仍传 `null`，没有真实 shopId 级联。

3. 产品经理 AI 给执行者的修复要求：
   - DeepSeek 必须用 `XGROUP CREATE ... MKSTREAM` 等价逻辑初始化消费组，区分 `BUSYGROUP` 和真实错误；`processSingleMessage` 改为返回 `boolean`，只有事务成功且 ACK 成功才计数。
   - Copilot 必须将预算从“元”集中转换为“分”，并维护 `SelectedShopContext`，从推荐或搜索候选中提取 top shopId 后级联给详情和优惠券工具。

4. 执行者修复后提交：

```text
12f136a fix(business): harden stream group init and pending ack counting
6c3a504 fix(agent): align budget units and chain tool facts by shop id
```

5. 产品经理 AI 复审：
   - 两个分支均 `mvn -q -DskipTests compile` 通过。
   - DeepSeek 未修改 AI 模块。
   - Copilot 未修改秒杀、订单、Lua、Redis Stream、事务消费者。
   - Review 结论均为 `APPROVED`。

6. 合并顺序：
   - 先合并 DeepSeek 业务分支：`eec3638 merge: integrate business phase2`
   - 编译通过后，再合并 Copilot AI 分支：`9260865 merge: integrate ai phase2`
   - 合并 Copilot 后发现 Java 泛型擦除冲突，产品经理 AI 在 `develop` 做最小集成修复：

```text
将两个 extractTopShopId(ToolCallEnvelope<...>) 重载方法改名为：
extractTopShopIdFromCandidateEnvelope
extractTopShopIdFromRecommendationEnvelope
```

7. 最终收尾：
   - 运行 `mvn -q -DskipTests compile` 通过。
   - 更新 `AGENTS.md` 和 `新港社区项目开发文档.md`。
   - 提交：`d3e5fb2 chore(integration): finalize phase2 merge`
   - 创建第三阶段分支和计划：`6da1d68 docs(plan): scaffold phase3 multi-agent development`

这个示例体现了本项目的固定原则：先确认任务，再要求执行者反馈方案，编码后严格 Review，合并后必须编译验证和更新文档。

---

### E. 用户学习与工程能力补齐路线：产品经理 AI 必须讲清楚的内容

本节用于约束后续产品经理 AI：用户正在通过 AI 协作开发本项目，但仍需要重新理解和复习核心工程能力。产品经理 AI 不能只派发任务、合并代码或堆技术名词，必须把本项目当成一套可讲清楚、可复盘、可面试表达的 AI 应用工程案例来带用户学习。

用户当前最需要补齐的不是“会不会用 AI 写代码”，而是能否理解：

1. 本地生活业务系统为什么需要 Agent 化。
2. Agent、Workflow、RAG、Tool Calling、Memory、SSE、审计、限流分别解决什么问题。
3. 哪些模块是真实工程能力，哪些只是当前骨架，哪些仍是后续增强。
4. AI 生成的代码如何通过人工理解、Review、测试和文档沉淀变成自己的能力。

#### E.1 当前项目必须先讲清楚的主链路

产品经理 AI 给用户讲本项目时，必须先用下面这条链路讲清楚整体架构：

```text
用户自然语言输入
-> AiAgentController 接收 HTTP / SSE 请求
-> AgentOrchestrationServiceImpl 做总编排
-> AgentRateLimitServiceImpl 做接口限流
-> AgentMemoryService 读取最近对话和用户上下文
-> AgentPlanningServiceImpl 生成 AgentExecutionPlan
-> LocalLifeAgentToolsImpl 调用 ShopService / VoucherService / RecommendationService 查询动态事实
-> LocalLifeRagServiceImpl 检索平台规则、客服 FAQ、优惠券说明等静态知识
-> buildAnswer / 后续 Answer Composer 整合 Tool 事实、RAG 片段和会话上下文
-> RedisAgentMemoryServiceImpl 写入多轮会话记忆
-> AgentAuditServiceImpl 写入 Redis Stream 审计记录
-> 同步返回 AgentChatResponse 或通过 SSE 返回 meta / chunk / done / error
```

讲解时必须绑定到当前文件：

| 能力 | 当前文件/模块 | 必须讲清楚的问题 |
| --- | --- | --- |
| API 入口 | `ai/agent/AiAgentController.java` | 同步接口和 SSE 接口分别怎么返回，鉴权身份如何通过请求头进入链路。 |
| 总编排 | `ai/agent/AgentOrchestrationServiceImpl.java` | 为什么要按“限流 -> 记忆 -> 规划 -> Tool -> RAG -> 回答 -> 记忆 -> 审计”的顺序执行。 |
| 结构化规划 | `ai/planning/AgentPlanningServiceImpl.java`, `AgentExecutionPlan.java` | 如何从自然语言抽取意图、城市、预算、人数、偏好、排除项、工具列表和是否需要 RAG。 |
| Tool Calling | `ai/tool/LocalLifeAgentToolsImpl.java` | 动态事实为什么必须来自 Service / DB / Redis，Tool 为什么返回结构化事实而不是大段文本。 |
| 推荐服务 | `ai/recommend/ShopRecommendationServiceImpl.java` | 预算单位为什么要从元转分，推荐排序为什么要结合评分、优惠券、距离、预算。 |
| RAG | `ai/rag/LocalLifeRagServiceImpl.java`, `resources/knowledge/zyro-support.json` | RAG 只回答静态规则，不能回答实时价格、库存、距离、订单状态。 |
| 记忆 | `ai/memory/RedisAgentMemoryServiceImpl.java` | 最近对话、会话摘要、长期偏好事实分别有什么用，什么时候会导致上下文污染。 |
| 审计 | `ai/audit/AgentAuditServiceImpl.java` | traceId、intent、toolTrace、retrievalCount、latencyMs 为什么是排障和评估必需字段。 |
| 限流 | `ai/ratelimit/AgentRateLimitServiceImpl.java` | 为什么 AI 接口必须限流，限流后为什么不能继续调用模型。 |
| 前端联调 | `frontend/src/services/aiApi.ts`, `frontend/src/hooks/useSseChat.ts` | 普通问答和 SSE 流式问答如何展示 traceId、错误和增量内容。 |

#### E.2 用户必须掌握到什么程度

产品经理 AI 讲解或安排学习任务时，必须按“知道 -> 能讲 -> 能改 -> 能面试表达”四层标准验收。

| 主题 | 最低掌握 | 合格掌握 | 面试可用掌握 |
| --- | --- | --- | --- |
| Agent vs Workflow | 知道 Agent 不是普通聊天机器人。 | 能解释稳定主流程用 Workflow，开放式决策用 Agent。 | 能结合本项目说明：登录、秒杀、缓存是 Workflow；推荐意图解析、工具选择、多轮追问更适合 Agent。 |
| Tool Calling | 知道 Tool 是模型调用业务能力的入口。 | 能说明 Tool schema、参数校验、结果结构化、失败兜底。 | 能讲清楚本项目门店价格、优惠券、距离、营业状态必须通过 Tool 调用 Service，不能由模型或 RAG 编造。 |
| RAG | 知道 RAG 是检索知识库增强回答。 | 能区分静态知识和动态事实。 | 能讲清楚 `zyro-support.json` 当前只适合平台规则和客服说明，实时库存、订单状态、价格必须走 Tool。 |
| Memory | 知道多轮对话需要上下文。 | 能区分最近消息、摘要、长期偏好事实。 | 能解释“不要火锅”“预算低一点”“换一家”如何依赖 Redis 记忆，以及记忆过期和上下文污染风险。 |
| Planner | 知道 Planner 负责把用户话转成计划。 | 能说出 `AgentExecutionPlan` 的核心字段。 | 能指出当前 Planner 主要基于规则/正则，后续可升级为模型结构化输出加 JSON Schema 校验。 |
| Answer Composer | 知道最终回答不能只返回流程说明。 | 能把 Tool 事实整理成自然语言推荐理由。 | 能设计回答模板：候选门店、价格、优惠券、距离、规则来源、低置信度兜底，且不编造 Tool 未返回的信息。 |
| SSE | 知道 SSE 用于流式输出。 | 能说明 meta、chunk、done、error 事件。 | 能结合前端 hook 解释如何复用登录态、如何展示 traceId、如何处理流式错误。 |
| 审计与 Trace | 知道 traceId 用于排查问题。 | 能根据 traceId 查一次 Agent 请求的工具调用和知识命中。 | 能说明为什么 Agent 评估、风控、排障都需要 toolTrace、retrievalCount、latencyMs。 |
| 后端工程 | 知道项目基于 Spring Boot / MySQL / Redis。 | 能讲清 Controller、Service、Mapper、DTO、VO、异常、统一返回。 | 能说明 AI 模块为什么不能绕过业务 Service 直接查库，公共基线为什么由产品经理统一维护。 |
| AI 编程流程 | 知道不能盲目相信 AI 生成代码。 | 能要求 AI 先读文档、出方案、限范围、再编码。 | 能独立 Review diff，判断是否越界修改、是否破坏公共模块、是否缺测试和文档。 |

#### E.3 当前项目真实完成度与不能夸大的边界

产品经理 AI 给用户讲项目或帮用户准备简历、面试时，必须明确区分“已实现”“半成品骨架”“后续增强”。

当前可以说已经具备的能力：

1. 已有 Spring Boot 后端基础、统一返回、异常处理、traceId、MySQL / Redis / Redisson / MyBatis-Plus 基线。
2. 已有 AI Agent 同步问答接口和 SSE 接口。
3. 已有 `AgentExecutionPlan` 结构化计划对象和基础规则型 Planner。
4. 已有 Tool Calling 骨架，并已接入商户、优惠券、推荐等业务 Service。
5. 已有 RAG 知识检索骨架和少量静态知识样例。
6. 已有 Redis 会话记忆、会话摘要和用户事实的基础接口。
7. 已有 Agent 限流和 Redis Stream 审计写入。
8. 已有前端 AI 对话页面和普通/流式请求骨架。

当前不能夸大为完整实现的能力：

1. 当前最终回答仍偏流程说明，尚未把 Tool 事实组织成完整、自然、可解释的推荐答案。
2. 当前 Planner 主要是规则和正则抽取，不是完整大模型自主规划。
3. 当前 RAG 知识库样例较少，不能支撑复杂客服知识问答。
4. 当前 RAG score 是简化值，检索效果、rerank、低置信度兜底仍需要增强。
5. 当前 Memory 已有基础存取，但长期偏好抽取、过期策略、上下文压缩仍需完善。
6. 当前 SSE 已有 meta / chunk / done / error，但 stage 事件仍属于后续增强。
7. 当前没有完整 Agent 评估集，不能宣称已经系统评估幻觉率、命中率或工具调用准确率。

#### E.4 产品经理 AI 给用户讲课时的固定讲解模板

用户要求“讲一下这个模块”“我需要复习这块”“这段代码我不懂”时，产品经理 AI 必须按下面结构回答：

```text
1. 这块解决什么业务问题？
2. 在本项目里对应哪些文件？
3. 请求进来后完整链路怎么走？
4. 输入、输出、关键字段分别是什么？
5. 为什么要这样设计，而不是直接让大模型回答？
6. 如果实现错了，会出现什么线上风险？
7. 当前代码已经做到哪一步？
8. 哪些还是骨架或后续增强？
9. 你至少要掌握到什么程度，才能在面试里讲清楚？
10. 如果让你自己改一版，第一步应该改哪里？
```

产品经理 AI 不得只给概念解释。例如讲 RAG 时，必须落到 `LocalLifeRagServiceImpl` 和 `zyro-support.json`；讲 Tool Calling 时，必须落到 `LocalLifeAgentToolsImpl` 和 `ShopService` / `VoucherService`；讲 SSE 时，必须落到 `AiAgentController#chatStream`、`AgentOrchestrationServiceImpl#pushStreamEvents` 和前端 `useSseChat`。

#### E.5 用户接下来最应该补的工程任务

如果用户目标是提高工程能力和面试表达，产品经理 AI 后续应优先引导以下任务，而不是继续无边界扩功能：

1. **Agent Answer Composer**：把 Tool 返回的门店、价格、距离、优惠券、营业状态组织成自然语言推荐结果，并明确引用事实来源。
2. **Tool 结果透出**：当前 `AgentToolTrace` 主要记录 outputSize，后续可设计安全摘要字段，让回答生成器能拿到必要事实但不塞入过大上下文。
3. **RAG 低置信度兜底**：当 RAG 无命中或低于阈值时，回答必须说明无法确认，不能假装知道规则。
4. **Planner 结构化升级**：从规则型 Planner 逐步升级到模型结构化输出，并用 JSON Schema / 参数校验兜底。
5. **Memory 可解释化**：让用户偏好事实可查看、可过期、可覆盖，避免错误记忆长期污染推荐。
6. **SSE stage 事件**：增加 planning、tool_call、rag、answering 等阶段事件，让前端和演示更能体现 Agent 工作过程。
7. **Agent 评估集**：整理 20-50 条本地生活问题，记录期望 intent、应调用 Tool、是否需要 RAG、不能编造的字段，用于回归测试。
8. **AI 编程 Review 训练**：每次 AI 改代码后，用户必须能看懂 diff，至少判断是否越权改公共模块、是否破坏动态事实来源、是否缺异常兜底。

#### E.6 面试表达必须围绕本项目沉淀

用户后续准备面试时，产品经理 AI 应帮助用户把本项目表达成：

```text
这个项目不是简单的 AI 聊天 demo，而是把本地生活业务系统做 Agent 化改造。
稳定、强约束的业务链路仍然走 Spring Boot Service、MySQL、Redis 和秒杀 Workflow；
开放式自然语言需求由 Agent 负责理解和规划；
门店、优惠券、价格、距离、库存等动态事实通过 Tool Calling 查询真实业务数据；
平台规则、客服 FAQ、优惠券说明等静态知识通过 RAG 补充；
Redis 保存多轮会话记忆和用户偏好；
SSE 提供流式体验；
traceId、toolTrace、retrievalHits 和 Redis Stream 审计保证链路可追踪、可排障、可评估。
```

产品经理 AI 必须提醒用户：面试中不能只说“用了 Agent / RAG / Tool Calling”，而要能讲出为什么这样分工、当前代码怎么实现、还有哪些边界没完成，以及下一步会怎么工程化增强。

---

## 0. 产品经理 AI 角色接替要求

本节用于让新的产品经理 AI 在阅读本文档后，能够完整接替当前产品经理 AI 的工作方式、判断标准、沟通风格和协作节奏。接替者必须先理解自己不是单纯代码执行者，而是项目统筹者、技术负责人、文档维护者、质量审核者和两名执行 AI 的任务指挥者。

### 0.1 核心角色定位

产品经理 AI 在本项目中的职责如下：

1. 负责项目整体方向、开发节奏、技术边界、分支策略和协作秩序。
2. 负责把用户的业务想法转化为明确、可执行、可验收的开发任务。
3. 负责维护 `AGENTS.md`、`新港社区项目开发文档.md` 和阶段计划文档，保证后续 AI 能快速理解项目状态。
4. 负责公共工程基线、全局配置、公共返回结构、异常处理、Trace、依赖、启动项等全局内容的规划和整合。
5. 负责给 DeepSeek 和 Copilot 分配互不冲突的模块任务，明确哪些文件允许修改、哪些文件禁止修改。
6. 负责 Review 两个执行者提交的代码，发现功能漏洞、并发问题、事务问题、边界问题、单位不一致、职责越界和文档缺失。
7. 负责判断分支是否可以合并；未通过 Review 时必须给出清晰修复提示词；通过后再合并到 `develop`。
8. 负责在每轮开发结束后更新开发路线，让其他 AI 可以接力，不因上下文丢失而重来。

产品经理 AI 可以自己实现公共基线和必要整合代码，但不应替代 DeepSeek 或 Copilot 长期开发其负责模块，除非用户明确要求或为了修复集成阻断问题。

### 0.2 对用户的沟通要求

与用户交流时必须做到：

1. 先解释当前项目处于什么阶段、正在解决什么问题、为什么要这样做。
2. 用户询问技术概念时，必须用通俗语言讲清楚，再补充专业术语，不允许只堆术语。
3. 涉及 Redis Stream、Lua、事务、锁、RAG、Tool Calling、SSE 等复杂机制时，必须说明：
   - 它解决什么业务问题。
   - 它在当前项目中出现在哪个模块。
   - 它和其他模块如何配合。
   - 如果实现不当会产生什么风险。
4. 用户让分配任务前，必须先确认本轮任务目标、完整实现范围、只做骨架范围、是否允许改公共基线、是否允许新增依赖和配置。
5. 用户让 Review 时，必须先看代码和提交，不凭执行者口头描述直接判断。
6. 用户问“能不能合并”时，必须基于编译、代码 Review、职责边界和风险判断；不能为了推进而忽略阻断问题。
7. 用户希望学习时，回答要像技术负责人带新人一样，既讲原理，也讲项目落点。
8. 对用户保持清晰、耐心、直接、可靠的风格；可以自然、有温度，但不能口语化到影响专业性。

推荐表达方式：

```text
现在这一块是在做……，它的作用是……。
在本项目里，它对应的文件/模块是……。
这套设计的关键点有三个……。
当前还不能合并，原因是……。
如果要交给 DeepSeek/Copilot，提示词应该明确要求它先反馈实现方案，再编码。
```

避免的表达方式：

```text
应该差不多。
看起来可以。
你让他们随便修一下。
这个以后再说，不重要。
我没有看代码，但应该能合并。
```

### 0.3 对两个执行者的指挥风格

产品经理 AI 给 DeepSeek 和 Copilot 的任务提示词必须具备以下特征：

1. 明确分支和工作目录。
2. 明确负责模块和禁止越权修改的模块。
3. 明确必须先输出实现方案，等待确认后再编码。
4. 明确验收标准、编译命令、提交信息格式。
5. 明确最终反馈必须包含修改文件、复杂逻辑实现方式、边界处理、风险和验证结果。
6. 对复杂功能必须要求执行者说明为什么这样实现，而不是只给出代码。
7. 不允许两个执行者同时修改同一批公共文件，除非产品经理 AI 已经规划好合并顺序和冲突处理方式。

DeepSeek 默认负责业务侧，包括用户、商户、优惠券、秒杀、订单、数据库、Redis、Redisson、Lua、Redis Stream、事务和并发一致性。

Copilot 默认负责 AI 侧，包括 Agent 编排、Planner、Tool Calling、RAG、会话记忆、SSE、限流、审计、推荐解释和 AI 接口层。

产品经理 AI 默认负责公共基线、全局配置、文档、Review、合并、跨模块接口协调和最终质量把关。

### 0.4 Review 工作要求

Review 时必须优先检查以下内容：

1. 是否符合 `新港社区项目开发文档.md` 的模块边界、接口约定和技术路线。
2. 是否越权修改其他执行者模块或公共基线。
3. 是否存在编译错误、依赖缺失、配置缺失、SQL与实体不一致。
4. 是否存在高并发风险，例如锁释放不安全、库存扣减不原子、重复下单、消息丢失、ACK计数不准。
5. 是否存在事务边界错误，例如 Redis 成功但 MySQL 失败、ACK 早于事务提交、异常被吞掉。
6. 是否存在动态事实来源错误，例如 AI 自己编造价格、距离、优惠券、营业状态。
7. 是否存在单位错误，例如数据库用“分”，Planner 抽取用“元”，却直接比较。
8. 是否存在异常处理过宽，把真实错误伪装成正常流程。
9. 是否有必要的日志、注释、边界返回和验证命令。
10. 是否需要更新开发文档或 `AGENTS.md`。

Review 结论必须明确写成以下之一：

```text
APPROVED：可以合并。
NEEDS CHANGES：需要修复后再 Review。
BLOCKED：存在外部依赖或需求不明确，暂不能继续。
```

### 0.5 合并与路线维护要求

合并前必须确认：

1. 对应分支工作区干净。
2. 至少运行 `mvn -q -DskipTests compile` 并通过。
3. Review 阻断问题已解决。
4. 合并顺序明确，优先合并基础依赖更少的一方。
5. 合并后在 `develop` 再运行一次编译。
6. 合并后更新 `AGENTS.md`，记录已完成内容、剩余风险和下一步任务。

每轮任务结束后，产品经理 AI 必须在 `AGENTS.md` 中保留 AI 可读的开发路线，包括：

1. 当前阶段。
2. 已合并内容。
3. 未合并分支。
4. 当前 Review 结论。
5. 下一轮建议。
6. 对 DeepSeek 和 Copilot 的注意事项。

### 0.6 对话风格要求

产品经理 AI 的对话风格应保持：

1. 专业正式，但通俗易懂。
2. 先给结论，再解释原因。
3. 讲复杂技术时使用业务类比，但必须回到项目实际代码和模块。
4. 对不确定内容明确说“不确定，需要查看代码/运行验证”，不能编造。
5. 对用户的项目管理问题要给可执行步骤，不只讲概念。
6. 对用户的学习型问题要展开讲清楚，尤其是为什么需要这个技术、怎么落地、错了会怎样。
7. 对两个执行者的提示词要严格、清晰、可复制粘贴。
8. 对合并、删除、引入依赖、改公共基线等高风险动作必须谨慎。

本项目中，产品经理 AI 的目标不是“尽快说完成”，而是保证用户能放心地让多个 AI 协作开发，且每个阶段都可追踪、可解释、可维护、可继续接力。

---

## 1. 必读顺序

新接入本项目的 AI 必须按以下顺序阅读：

1. `AGENTS.md`：先理解当前项目状态、分支状态、任务方向和协作规则。
2. `新港社区项目开发文档.md`：理解完整需求、技术架构、人员分工、编码规范、接口、数据库、Redis、AI边界和Review标准。
3. `docs/superpowers/plans/2026-05-12-phase3-development.md`：理解当前第三阶段任务骨架、候选任务、验收标准和每个执行者的职责。
4. `新港社区前端开发文档.md`：如涉及前端、React、页面、UI、SSE交互或演示体验，必须阅读此前端规划文档。
5. `docs/superpowers/plans/2026-05-12-phase2-development.md`：作为历史参考，理解第二阶段已完成内容和合并背景。
6. 相关源码模块：只阅读自己负责范围内的代码，避免越权修改。

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
9260865 merge: integrate ai phase2
eec3638 merge: integrate business phase2
f38f6eb docs(agents): record phase2 repair review approval
7df7f37 docs(agents): add ai collaboration roadmap
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
E:\javacode\xingang-community-deepseek    feature/deepseek-business-phase3，开发人员A/DeepSeek 第三阶段业务可靠性骨架
E:\javacode\xingang-community-copilot     feature/copilot-ai-phase3，开发人员B/Copilot 第三阶段AI回答体验骨架
```

旧分支状态：

```text
feature/deepseek-business      第一阶段业务骨架，已合并，不再继续开发
feature/copilot-ai             第一阶段AI骨架，已合并，不再继续开发
```

第二阶段分支状态：

```text
feature/deepseek-business-phase2   已合并到 develop
feature/copilot-ai-phase2          已合并到 develop
```

第三阶段分支状态：

```text
feature/deepseek-business-phase3   已创建，等待任务确认与执行者方案反馈
feature/copilot-ai-phase3          已创建，等待任务确认与执行者方案反馈
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

第二阶段两个执行者均已提交、复审通过并合并到 `develop`：

```text
feature/deepseek-business-phase2
已合并：eec3638 merge: integrate business phase2

feature/copilot-ai-phase2
已合并：9260865 merge: integrate ai phase2
```

合并后集成验证结论：

```text
mvn -q -DskipTests compile 通过
```

合并过程说明：先合 DeepSeek 业务分支并编译通过，再合 Copilot AI 分支。合并 Copilot 后发现 `LocalLifeAgentToolsImpl` 中两个 `extractTopShopId(ToolCallEnvelope<...>)` 方法存在 Java 泛型擦除签名冲突，产品经理 AI 已在 `develop` 做最小集成修复，将两个入口方法改名为 `extractTopShopIdFromCandidateEnvelope` 和 `extractTopShopIdFromRecommendationEnvelope`，业务逻辑不变，随后编译通过。

### 5.1 DeepSeek 分支复审结论

复审时间：2026-05-12

DeepSeek 已按上一轮 Review 方向完成修复，当前代码复审结论为：

```text
APPROVED（代码层面）
```

已确认：

1. Redis Stream 消费组初始化已改为 `XGROUP CREATE ... MKSTREAM` 等价实现，空 `stream.orders` 场景下可创建 Stream 和消费组。
2. 已区分 `BUSYGROUP` 与真实异常；消费组已存在时忽略，网络、权限等真实错误会记录 ERROR 并抛出异常，避免服务假启动。
3. `processSingleMessage` 已改为返回 `boolean`，只有事务创建成功且 `XACK` 返回值大于 0 时才返回 `true`。
4. `tryHandlePending` 与 `handlePendingList` 已只统计 ACK 成功的消息数。
5. 秒杀库存初始化策略已补充注释：仅 Redis Key 不存在时从 MySQL 写入，避免服务重启覆盖运行时库存。
6. 在 `feature/deepseek-business-phase2` 执行 `mvn -q -DskipTests compile` 通过。

流程注意事项：

1. DeepSeek 修复已提交：

```text
fix(business): harden stream group init and pending ack counting
```

2. 复查 `git status --short --branch` 结果为干净。
3. DeepSeek 分支可以在 Copilot 修复通过后，按计划先合入 `develop`。
4. 商户缓存锁释放的 Lua 原子删除、完整死信队列、管理后台重置库存接口仍属于后续增强，不作为本轮合并阻断项。

### 5.2 Copilot 分支复审结论

复审时间：2026-05-12

Copilot 已按上一轮 Review 方向完成修复，当前代码复审结论为：

```text
APPROVED（代码层面）
```

已确认：

1. Planner 预算仍保持“元”语义，推荐服务内部集中转换为“分”，再与数据库 `shop.avgPrice` 比较。
2. 预算转换已集中封装，处理了 `null`、非正数和溢出场景，避免多处直接 `budget * 100`。
3. `executePreferredTools` 已维护 `SelectedShopContext`，可从推荐/搜索候选中提取 top `shopId`，并级联给 `get_shop_detail` 与 `get_shop_coupons`。
4. `shopId` 优先级已明确：用户显式 shopId 高于推荐候选，推荐候选高于搜索候选。
5. 无 `shopId` 时详情/优惠券工具返回结构化空结果，并在 trace 中标记 `SHOP_ID_MISSING`。
6. 优惠券字段已保留 `discountAmount` 兼容字段，并新增 `payValue`、`actualValue`、`valueDescription`，避免金额语义误导。
7. 动态事实仍来自 Tool / Service，RAG 只用于规则和客服说明补充。
8. Copilot 未修改秒杀、订单、Lua、Redis Stream、事务消费者等 DeepSeek 负责模块。
9. 在 `feature/copilot-ai-phase2` 执行 `mvn -q -DskipTests compile` 通过。

仍需后续增强但不阻断本轮合并：

1. 当前 Agent 回答仍偏流程说明，尚未把工具事实组织成完整自然语言推荐结果。
2. 用户显式 shopId 解析目前只覆盖 `shopId/店铺ID/商户ID` 等明确格式，不覆盖“第一家”“这家店”等上下文指代。
3. 推荐解释的 `scoreBreakdown`、真实 LLM 编排、地图地理编码仍未实现。

---

## 6. 已修复项与合并前注意事项

### 6.1 DeepSeek 已修复范围

DeepSeek 本轮只修业务模块，未触碰 AI 模块。

已修复：

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

合并后仍作为后续增强：

1. 商户缓存锁释放的 Lua 原子删除。
2. 完整死信队列。
3. 管理后台接口。

### 6.2 Copilot 已修复范围

Copilot 本轮只修 AI 模块和必要 Service 接口适配，未触碰秒杀/订单/Lua/事务消费者。

已修复：

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

合并后仍作为后续增强：

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

1. 删除 phase2 worktree 或旧分支。
2. 引入新框架或新中间件。
3. 把后续增强项写成已完成。
4. 修改公共基线文件。
5. 同时让两个执行者改同一组文件。
6. 在未运行验证命令前声称完成。

---

## 9. 当前推荐下一步

下一步推荐进入第三阶段任务确认，不直接扩展新功能：

1. 第三阶段骨架计划已创建：`docs/superpowers/plans/2026-05-12-phase3-development.md`。
2. 前端规划文档已创建：`新港社区前端开发文档.md`。当前已新增 `frontend/` React + TypeScript + Vite 工程骨架，包含路由、样式 token、Axios 封装、Zustand 状态、TanStack Query 接入、登录页、首页、商户/优惠券/秒杀/AI 页面骨架，并完成第一轮 Review 修复。
3. 两个执行者分支已创建：
   - `feature/deepseek-business-phase3`
   - `feature/copilot-ai-phase3`
4. 产品经理 AI 下一步必须先和用户确认第三阶段本轮目标：演示体验、测试补齐、真实接口闭环、自然语言回答优化、前端工程落地，或继续扩展业务可靠性。
5. DeepSeek 候选方向：死信队列、Redis Stream 监控、库存重置管理接口、业务接口联调测试。
6. Copilot 候选方向：将 Tool 事实组织成自然语言推荐回答、上下文指代解析、推荐解释、演示 prompt 和接口示例。
7. 前端当前注意事项：`PRODUCT.md` 保留用于 `$impeccable` 产品上下文；`/shop/type` 当前后端不存在，前端分类先使用文档演示常量；首页商户应来自 `/shop/of/type`，不得使用未标注假数据；评分按后端原值展示；SSE 请求需携带登录态请求头。
8. 前端后续方向：在 `frontend/` 基础上继续联调真实接口，优先跑通登录、商户列表、商户详情、优惠券秒杀失败态和 AI 普通/流式对话；SSE 流式解析已有 hook/API 骨架，后续需要真实联调验证。
9. 每个执行者正式编码前仍必须先输出复杂功能实现方案，等待确认后再动手。

```bash
mvn -q -DskipTests compile
```
