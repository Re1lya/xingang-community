# 新港社区：AI 本地生活服务与智能客服平台魔改方案

## 1. 项目命名与定位

### 项目名称

```text
新港社区
```

简历中可以写成：

```text
新港社区：AI 本地生活服务与智能客服平台
```

这里参考截图里的项目命名，但方案内容以当前 `zyro-local` 仓库已经具备的技术栈和实现为主，不强行套用截图中的 `RocketMQ`、`LangChain4j` 等未接入技术。

### 项目简介

```text
新港社区是一个面向本地生活场景的 AI 应用后端项目，为用户提供商户查询、优惠券秒杀、附近门店推荐、探店内容查询、智能客服和平台规则问答等能力。项目基于 Spring Boot + Spring AI + MySQL + Redis + Redisson + MyBatis-Plus 构建，在保留黑马点评业务中的 Redis 缓存、秒杀券、一人一单、异步下单、分布式锁等后端能力基础上，接入大模型实现结构化规划、Tool Calling 业务查询、RAG 知识检索、Redis 多轮会话记忆和 SSE 流式输出。
```

### 核心定位

不要写成：

```text
黑马点评 + AI 聊天机器人
```

推荐写成：

```text
基于 Spring Boot + Spring AI 对本地生活点评业务进行 Agent 化改造，围绕商户查询、优惠券秒杀、门店推荐、智能客服和平台规则解释场景，构建“结构化规划 + Tool Calling + RAG + Redis 会话记忆 + SSE 流式输出 + Agent 审计”的 AI 应用后端系统。
```

## 2. 当前项目真实技术栈

### 2.1 已使用技术栈

```text
Java 21
Spring Boot 3.5.x
Spring AI 1.1.x
MySQL
Redis
Redis Stream
Lua
Redisson
MyBatis-Plus
SSE
Actuator
Prometheus
Baidu Map API
DashScope Embedding / Rerank
OpenAI-compatible Chat Model
```

### 2.2 不建议写成已实现的技术

以下技术可以作为后续增强，但当前文档不要写成主实现：

```text
RocketMQ
LangChain4j
Sa-Token
Milvus
Elasticsearch
MCP
支付网关
订单支付 / 超时关单完整闭环
```

原因很简单：当前仓库主线是 `Spring AI + Redis Stream + 本地 / OpenAI-compatible RAG`，不是截图第二个 RAG 平台，也不是 RocketMQ 秒杀项目。简历可以美化，但最好不要把没有的技术写成已经做完的实现，面试追问时更稳。

## 3. 当前项目已有实现基础

| 能力 | 当前落点 | 可包装价值 |
| --- | --- | --- |
| 用户登录 | `UserController`, Redis token | 登录态、鉴权、会话用户上下文 |
| 商户查询 | `ShopController`, `ShopServiceImpl` | 本地生活业务底座 |
| 商户缓存 | `CacheClient`, `RedisConstants` | 缓存穿透、击穿、逻辑过期 |
| 商户 GEO | `SHOP_GEO_KEY`, Redis GEO 查询 | 附近门店查询 |
| 优惠券秒杀 | `VoucherOrderServiceImpl`, `lua/seckill.lua` | Redis + Lua 防超卖、一人一单 |
| 异步下单 | Redis Stream `stream.orders` | 秒杀请求削峰、异步创建订单 |
| 分布式锁 | Redisson `lock:order:{userId}` | 防重复消费和并发重复下单 |
| Agent API | `AiAgentController` | `/ai/agent/chat`, `/ai/agent/chat/stream` |
| 结构化规划 | `AgentPlanningServiceImpl`, `AgentExecutionPlan` | 用户意图解析和工具选择 |
| Tool Calling | `LocalLifeAgentTools` | 商户、优惠券、位置、推荐工具 |
| RAG | `LocalLifeRagService`, `AiKnowledgeServiceImpl` | 平台规则和客服知识检索 |
| 推荐服务 | `ShopRecommendationServiceImpl` | 预算、位置、语义召回、优惠筛选 |
| 会话记忆 | `RedisChatMemoryRepository`, `RedisChatContextRepository` | 多轮对话记忆、长期偏好事实 |
| 上下文压缩 | `ContextCompressionService` | 长对话 token 控制 |
| Agent 限流 | `AgentRateLimitServiceImpl` | Redis 固定窗口限流 |
| Agent 审计 | `AgentAuditServiceImpl` | Redis Stream 异步记录调用链路 |
| 可观测 | Actuator, Prometheus, traceId | 健康检查和指标暴露 |

## 4. 简历主要工作：按当前实现改写

### 4.1 Java 后端与高并发方向

- 秒杀防超卖和一人一单：基于 Redis + Lua 实现秒杀资格校验，在脚本中原子判断库存、判断用户是否已下单、扣减库存并写入用户购买集合，避免高并发下库存超卖和重复下单。

- 秒杀异步下单：秒杀入口线程只完成资格判断、库存预扣和订单消息写入，Lua 脚本通过 `XADD` 将订单写入 Redis Stream `stream.orders`，后台消费组异步创建订单，降低接口响应耗时和数据库瞬时写入压力。

- Redis Stream 消费可靠性：服务启动时初始化消费组，后台线程阻塞读取订单消息；消费异常时处理 pending-list，订单创建成功后 ACK，降低消息丢失和重复消费风险。

- 分布式锁控制重复下单：异步创建订单前按用户维度加 Redisson 分布式锁 `lock:order:{userId}`，并在事务方法中二次校验一人一单和数据库库存，形成 Redis 前置校验 + MySQL 乐观扣减的双保险。

- 商户缓存优化：使用 Redis 缓存商户详情和商户分类，结合缓存空值解决缓存穿透，结合互斥锁和逻辑过期缓解热点商户缓存击穿，提升商户查询接口稳定性。

- 附近商户查询：使用 Redis GEO 维护不同分类下的商户坐标，通过经纬度和距离分页查询附近门店，为本地生活推荐 Agent 提供位置数据基础。

- AI 接口限流：基于 Redis 固定时间窗口计数实现 Agent 对话接口限流，分别控制普通对话和流式对话请求频率，避免模型调用被刷爆。

### 4.2 Agent 与 AI 应用方向

- 智能客服 RAG：使用 Spring AI 构建本地生活客服知识检索能力，将平台规则、优惠券说明、秒杀规则、推荐约束等静态知识放入知识库，通过向量检索为客服问答提供依据。

- Tool Calling 业务查询：将商户搜索、商户详情、优惠券查询、热门博客、当前位置、门店推荐、附近门店推荐等能力封装为 Spring AI Tool，由模型根据结构化计划调用 Java Service 查询真实业务数据。

- 结构化规划：通过 `AgentPlanningServiceImpl` 将自然语言解析为 `AgentExecutionPlan`，识别推荐、客服、事实查询等意图，并抽取城市、地点、预算、人数、偏好、排除项、推荐工具等字段。

- 门店推荐 Agent：基于 `ShopRecommendationServiceImpl` 综合数据库候选、Redis GEO、百度地图地理编码、语义餐厅检索、预算和优惠券筛选，返回真实门店候选，再由模型组织推荐理由。

- 动态事实与 RAG 边界治理：规定门店价格、评分、距离、优惠券、库存、营业状态等动态事实必须走 Tool / MySQL / Redis；RAG 只承载平台规则、客服说明和推荐约束，降低大模型幻觉风险。

- Redis 会话记忆和上下文压缩：使用 Redis 保存最近对话、会话摘要和长期偏好事实，结合 `ContextCompressionService` 对长对话进行摘要压缩，控制 token 成本并保留关键上下文。

- SSE 流式输出：通过 `/ai/agent/chat/stream` 提供流式响应，返回 `meta`、`chunk`、`done`、`error` 等事件，提升智能客服和推荐问答的交互体验。

- Agent 审计追踪：异步将 traceId、userId、conversationId、intent、retrievalCount、toolTrace、latencyMs 等字段写入 Redis Stream，方便联调排障和后续风控分析。

## 5. 项目总体架构

```text
用户请求
  ↓
API 层：登录鉴权 / traceId / HTTP / SSE
  ↓
业务层：商户查询 / 优惠券秒杀 / 探店博客 / 推荐服务
  ↓
缓存与并发层：Redis 缓存 / Redis GEO / Lua / Redis Stream / Redisson
  ↓
Agent 层：Planner 结构化规划 / Tool Calling / Redis 会话记忆
  ↓
RAG 层：平台规则 / 客服 FAQ / 优惠券说明 / 推荐约束
  ↓
LLM 层：基于工具结果和知识片段生成自然语言回答
  ↓
治理层：限流 / 审计 / trace / Actuator / Prometheus
```

核心边界：

```text
动态事实：Tool / DB / Redis
静态知识：RAG
表达生成：LLM
链路追踪：toolTrace + retrievalHits + audit stream
```

## 6. 核心模块魔改设计

### 6.1 秒杀优惠券模块

当前实现链路：

```text
用户抢券
  ↓
生成订单 ID
  ↓
执行 lua/seckill.lua
  ↓
校验库存是否充足
  ↓
校验用户是否已下单
  ↓
Redis 预扣库存
  ↓
SADD 记录用户已抢券
  ↓
XADD 写入 Redis Stream
  ↓
后台消费组异步读取订单
  ↓
Redisson 用户锁防重复处理
  ↓
事务内二次校验 + MySQL 乐观扣减库存 + 保存订单
  ↓
ACK 消息
```

关键 Redis Key：

```text
seckill:stock:{voucherId}
seckill:order:{voucherId}
stream.orders
lock:order:{userId}
```

简历亮点：

```text
使用 Redis + Lua 将库存校验、一人一单、预扣库存、订单消息入队合并为一次原子操作，并通过 Redis Stream 消费组异步创建订单，配合 Redisson 用户锁和数据库乐观扣减库存提高秒杀链路可靠性。
```

### 6.2 商户查询与缓存模块

当前项目适合重点讲：

```text
商户详情缓存
商户类型缓存
缓存空值防穿透
互斥锁 / 逻辑过期防击穿
Redis GEO 附近商户查询
数据库更新后删除缓存
```

推荐包装：

```text
针对商户详情和商户分类等高频读接口设计 Redis 缓存，使用缓存空值解决非法 ID 导致的缓存穿透，使用互斥锁和逻辑过期降低热点商户缓存击穿风险，并基于 Redis GEO 支持附近门店检索。
```

### 6.3 智能客服 RAG 模块

当前知识库入口：

```text
src/main/resources/knowledge/zyro-support.json
```

当前配置能力：

```text
AI_RAG_ENABLED
AI_RAG_STORE_FILE
AI_RAG_SIMILARITY_THRESHOLD
AI_KNOWLEDGE_TOP_K
AI_EMBEDDING_PROVIDER
AI_RERANK_ENABLED
```

适合 RAG 的问题：

```text
优惠券过期了还能用吗？
秒杀券抢到了在哪里看？
为什么推荐结果里没有某家店？
商家评分和评价有什么参考意义？
平台为什么不能保证历史价格和库存？
```

不适合 RAG 的问题：

```text
这张券现在还有库存吗？
这家店现在有什么优惠？
我附近 3 公里有哪些店？
这个商户今天营业吗？
```

这些要走 Tool。

### 6.4 Tool Calling 业务查询模块

当前已暴露的核心工具：

| Tool | 用途 |
| --- | --- |
| `search_shops` | 按关键词和分类查询商户 |
| `get_shop_detail` | 查询商户价格、评分、地址、营业时间等详情 |
| `get_shop_coupons` | 查询商户优惠券 |
| `get_hot_blogs` | 查询热门探店博客 |
| `get_current_user_location` | 获取当前用户城市、地址、坐标 |
| `recommend_shops` | 基础门店推荐 |
| `recommend_shops_v2` | 带城市、地点、预算、坐标的增强推荐 |
| `recommend_nearby_shops` | 附近门店推荐，支持模型选择距离半径 |

Tool 设计原则：

```text
Tool 返回结构化事实，不返回大段自由文本
Tool 结果写入 toolTrace，便于排查和面试演示
Tool 查询为空时不让模型编造候选
动态事实必须来自 Tool，而不是 RAG
```

### 6.5 自然语言推荐 Agent

用户输入：

```text
广州正佳附近有没有适合两个人约会、人均 80 以内、不要火锅的餐厅？
```

Planner 预期结构化结果：

```json
{
  "intent": "recommendation",
  "city": "广州",
  "locationHint": "正佳",
  "nearby": true,
  "partySize": 2,
  "budgetMax": 80,
  "scenePreference": "约会",
  "excludedCategories": ["火锅"],
  "preferredTools": ["recommend_nearby_shops", "get_shop_coupons"]
}
```

执行链路：

```text
Planner 解析结构化计划
  ↓
Redis 读取会话上下文和长期偏好事实
  ↓
百度地图解析显式地点坐标
  ↓
推荐服务查询 DB / GEO / 语义候选
  ↓
按预算、位置、优惠、排除项排序
  ↓
Tool 返回真实候选
  ↓
RAG 只补充推荐规则或平台解释
  ↓
LLM 生成最终推荐回答
```

推荐输出应该包含：

```text
门店名称
人均价格
评分
距离 / 商圈
优惠券情况
为什么符合需求
哪些条件无法保证
```

### 6.6 Redis 会话记忆与上下文工程

当前项目已经不是简单保存最近 N 轮对话，而是包含：

```text
最近对话窗口
会话摘要
长期偏好事实
MicroCompact 压缩
token budget 控制
记忆过期和相关性过滤
```

可以这样包装：

```text
基于 Redis 实现 Agent 会话上下文管理，保存最近多轮对话、会话摘要和长期偏好事实；通过上下文压缩和 token 预算控制，避免长对话中历史工具结果过大导致关键约束丢失。
```

多轮示例：

```text
第一轮：帮我推荐正佳附近适合约会的餐厅，人均 80 以内
第二轮：不要西餐，换成粤菜
第三轮：有没有券？
```

预期：

```text
第二轮继承正佳、约会、人均 80
新增排除西餐和偏好粤菜
第三轮基于上一轮候选查询优惠券
```

### 6.7 SSE 流式输出

当前接口：

```text
POST /ai/agent/chat/stream
```

当前事件：

```text
meta
chunk
done
error
```

可作为后续增强的事件：

```text
stage: planning
stage: tool_call
stage: rag
stage: generating
```

简历建议写法：

```text
基于 SSE 实现 Agent 流式响应，首个事件返回会话元信息和规划结果，中间持续返回模型增量内容，完成时返回最终答案，异常时返回统一错误事件。
```

### 6.8 Agent 审计与限流

当前限流实现：

```text
Redis 固定时间窗口计数
chat 和 stream 分别配置请求上限
key = ai:rate-limit:{scene}:{principalKey}:{bucket}
```

当前审计实现：

```text
异步写入 Redis Stream
默认 stream key = hmdp:agent:audit
记录 traceId、userId、conversationId、intent、toolTrace、retrievalCount、latencyMs
```

简历建议写法：

```text
为 Agent 接口增加 Redis 固定窗口限流和异步审计机制，区分普通对话与流式对话请求配额，并将规划结果、知识命中、工具轨迹和耗时写入 Redis Stream，方便联调排障和链路追踪。
```

## 7. API 设计

### 当前核心接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/user/login` | 用户登录 |
| `GET` | `/shop/{id}` | 商户详情 |
| `GET` | `/shop/of/type` | 按分类 / 位置查询商户 |
| `GET` | `/voucher/list/{shopId}` | 查询商户优惠券 |
| `POST` | `/voucher-order/seckill/{voucherId}` | 秒杀下单 |
| `POST` | `/ai/agent/chat` | 同步 Agent 对话 |
| `POST` | `/ai/agent/chat/stream` | SSE 流式 Agent 对话 |
| `DELETE` | `/ai/agent/session` | 清理当前用户会话 |
| `POST` | `/ai/agent/knowledge/rebuild` | 重建知识索引 |
| `GET` | `/actuator/health` | 健康检查 |
| `GET` | `/actuator/prometheus` | Prometheus 指标 |

### 后续可补齐接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/ai/user/preference` | 查询用户偏好画像 |
| `POST` | `/ai/user/preference/refresh` | 刷新用户偏好画像 |
| `POST` | `/ai/recommend/feedback` | 提交推荐反馈 |
| `GET` | `/ai/unresolved/list` | 查询未解决客服问题 |

## 8. 后续魔改增强项

这些是“下一步可做”，不要写成当前已完成。

### 8.1 用户偏好画像表

当前项目已有长期记忆事实，可以进一步落成用户画像表：

```sql
CREATE TABLE ai_user_preference (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  category_preference VARCHAR(512),
  price_preference VARCHAR(128),
  negative_tags VARCHAR(512),
  scene_preference VARCHAR(512),
  last_city VARCHAR(64),
  confidence DECIMAL(5, 2),
  updated_time DATETIME,
  UNIQUE KEY uk_user_id (user_id)
);
```

### 8.2 未解决问题表

用于低置信度客服兜底：

```sql
CREATE TABLE ai_unresolved_question (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  question VARCHAR(1024) NOT NULL,
  rag_score DECIMAL(8, 4),
  scene VARCHAR(64),
  status VARCHAR(32) DEFAULT 'pending',
  answer_snapshot TEXT,
  created_time DATETIME,
  updated_time DATETIME,
  INDEX idx_status_time (status, created_time)
);
```

### 8.3 推荐反馈表

用于优化推荐排序：

```sql
CREATE TABLE ai_recommend_feedback (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  conversation_id VARCHAR(128),
  shop_id BIGINT NOT NULL,
  feedback_type VARCHAR(32) NOT NULL,
  reason VARCHAR(512),
  created_time DATETIME,
  INDEX idx_user_time (user_id, created_time),
  INDEX idx_shop_time (shop_id, created_time)
);
```

### 8.4 推荐可解释评分增强

当前推荐服务已经包含业务侧排序和推荐理由，可以进一步显式输出 `scoreBreakdown`：

```text
recommend_score =
  rating_score * 0.25
  + distance_score * 0.20
  + budget_match_score * 0.20
  + coupon_score * 0.15
  + preference_match_score * 0.15
  + popularity_score * 0.05
  - excluded_penalty
```

输出解释：

```text
距离近：约 800m
人均符合：75 元，低于 80 元预算
评分较高：4.7
有优惠券
符合“约会、安静”的偏好
```

### 8.5 低置信度转人工

触发条件：

```text
RAG 无命中
RAG top1 分数低于阈值
问题涉及投诉、退款、赔付等高风险场景
模型无法基于现有规则确认
```

用户侧回答：

```text
这个问题我暂时无法基于现有平台规则确认。为了避免误导你，建议转人工客服，我也会把这个问题记录下来用于后续补充知识库。
```

## 9. 开发优先级

### 第一阶段：巩固当前已实现能力

```text
商户详情缓存
商户分类缓存
Redis GEO 附近查询
秒杀券 Redis 库存初始化
Lua 原子校验
Redis Stream 异步下单
Redisson 用户锁
Agent chat / stream 接口
RAG 知识索引
Tool trace 和 audit stream
```

### 第二阶段：补齐演示体验

```text
准备 10 条推荐 / 客服演示 prompt
补充 zyro-support.json 中的客服规则
优化推荐回答模板
SSE 增加 stage 事件
完善 README 和接口示例
增加 Postman / curl 验证脚本
```

### 第三阶段：差异化增强

```text
用户偏好画像接口
推荐 scoreBreakdown
低置信度转人工
未解决问题沉淀
推荐反馈闭环
更细的 Agent 指标统计
```

## 10. 演示用例

### 10.1 秒杀场景

```text
同一用户重复抢同一张秒杀券
```

预期：

```text
Lua 通过 SISMEMBER 判断用户已下单，返回重复下单错误
Redis 库存不会重复扣减
后台不会重复创建有效订单
```

### 10.2 门店推荐场景

```text
广州正佳附近有没有适合两个人约会、人均 80 以内、不要火锅的餐厅？
```

预期：

```text
Planner 识别 recommendation
抽取 city=广州、locationHint=正佳、budgetMax=80、partySize=2、excludedCategories=火锅
preferredTools 包含 recommend_nearby_shops / recommend_shops_v2
最终答案基于真实 Tool 候选，不编造门店
```

### 10.3 多轮追问场景

```text
第一轮：帮我推荐正佳附近适合约会的餐厅，人均 80 以内
第二轮：不要西餐，换成粤菜
第三轮：有没有券？
```

预期：

```text
第二轮继承地点、预算、场景
第三轮基于上一轮候选或相同约束查询优惠券
```

### 10.4 客服 RAG 场景

```text
秒杀券抢到了在哪里看？
```

预期：

```text
命中客服知识库
解释秒杀券、一人一单、异步下单等规则
不承诺实时库存或订单状态，实时状态需要 Tool 查询
```

## 11. 最终简历写法

### 项目名称

```text
新港社区
```

### 项目描述

```text
新港社区是一个面向本地生活场景的 AI 应用后端项目，为用户提供商户查询、优惠券秒杀、附近门店推荐、智能客服和平台规则问答等能力。项目基于 Java 21 + Spring Boot 3.x + Spring AI + MySQL + Redis + Redisson + MyBatis-Plus 构建，在保留 Redis 缓存、Lua 防超卖、一人一单、Redis Stream 异步下单、Redisson 分布式锁等后端能力的基础上，接入大模型实现 Tool Calling 业务查询、RAG 知识检索、Redis 多轮会话记忆和 SSE 流式输出。
```

### 简历 Bullet

1. 使用 Redis + Lua 实现优惠券秒杀资格校验，原子判断库存和用户下单状态，并在脚本中完成预扣库存、记录用户购买集合和写入 Redis Stream，避免高并发场景下库存超卖和一人多单。

2. 基于 Redis Stream 消费组实现秒杀订单异步创建，后台线程阻塞读取订单消息，异常时处理 pending-list，订单创建成功后 ACK，降低秒杀接口响应耗时和数据库瞬时写入压力。

3. 使用 Redisson 按用户维度加分布式锁，并在事务方法中二次校验一人一单和数据库库存，结合 MySQL 乐观扣减保证秒杀链路的并发安全。

4. 设计商户详情和商户分类 Redis 缓存，使用缓存空值解决缓存穿透，使用互斥锁和逻辑过期缓解热点 Key 击穿，并基于 Redis GEO 支持附近商户查询。

5. 基于 Spring AI 构建本地生活推荐 Agent，将用户自然语言解析为城市、地点、预算、人数、偏好、排除项等结构化计划，并通过 Tool Calling 调用商户查询、优惠券查询、位置查询和门店推荐等 Java Tool 获取真实业务数据。

6. 明确区分 Tool Calling 与 RAG 边界，将门店价格、距离、库存、优惠券等动态事实交由 MySQL / Redis 查询，平台规则、客服 FAQ、秒杀说明等静态知识通过 RAG 检索补充，降低模型幻觉风险。

7. 基于 Redis 实现 Agent 多轮会话记忆、会话摘要和长期偏好事实，并通过上下文压缩控制 token 成本，支持“换一家”“不要火锅”“预算低一点”“有没有券”等上下文追问。

8. 基于 SSE 实现 Agent 流式响应，并增加 Redis 固定窗口限流和 Redis Stream 异步审计，记录 traceId、意图、知识命中、工具轨迹和耗时，提升 Agent 链路的可观测性和稳定性。

## 12. 面试表达

可以这样说：

```text
这个项目不是简单把黑马点评接一个聊天机器人，而是把本地生活业务系统 Agent 化。后端部分我保留并增强了 Redis 缓存、优惠券秒杀、Lua 防超卖、一人一单、Redis Stream 异步下单和 Redisson 分布式锁；AI 部分我把商户、优惠券、位置、推荐这些动态事实封装成 Tool，把平台规则和客服 FAQ 放到 RAG，模型只负责理解、编排和表达。这样既能体现 Java 后端工程能力，也能体现 AI 应用落地能力。
```

更短版本：

```text
我的方向不是纯算法，而是 AI 应用后端。这个项目主要解决如何把 Agent、RAG、Tool Calling 接入真实 Java 业务系统，同时保证动态数据可信、推荐结果可解释、服务链路稳定。
```

## 13. 最小验收清单

```text
[ ] /voucher-order/seckill/{voucherId} 能通过 Lua 返回订单 ID
[ ] Redis 中存在 seckill:stock:{voucherId} 和 stream.orders
[ ] 重复抢券能返回重复下单
[ ] Redis Stream 消费组能异步创建订单并 ACK
[ ] /ai/agent/chat 能返回 answer、plan、toolTrace、retrievalHits
[ ] /ai/agent/chat/stream 能返回 meta、chunk、done
[ ] 推荐问题能稳定识别为 recommendation
[ ] 动态事实来自 Tool，而不是 retrievalHits
[ ] RAG 命中只用于规则解释
[ ] Agent 审计写入 hmdp:agent:audit
[ ] /actuator/health 和 /actuator/prometheus 可用
```
