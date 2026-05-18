# Phase 3 AI 回答体验演示示例

## 1. 演示 Prompt 示例

1. 推荐场景：
`帮我在深圳南山推荐适合2人约会的粤菜，人均80元以内，顺便看看有没有可用券。`

2. 附近推荐场景：
`我在科技园附近，推荐3公里内评分高一点的晚餐店。`

3. 详情查询场景（带显式 shopId）：
`请查询 shopId=17 的店铺详情和优惠券。`

4. 规则咨询场景（RAG）：
`新港社区优惠券使用规则是什么？`

> 说明：价格、距离、营业状态、优惠券等动态事实必须来自 Tool/Service 查询结果；RAG 仅补充平台规则和客服说明。

## 1.1 Tool 执行状态文案约定

- Tool 未执行：`本次未查询店铺详情。` / `本次未查询优惠券信息。`
- Tool 已执行但无结果：`已查询店铺详情，但暂未返回有效详情数据。` / `已查询该店铺优惠券，但当前未返回可用优惠券。`
- Tool 已执行且有结果：输出店铺详情与优惠券结构化事实文本。

## 2. 接口调用示例（curl）

### 2.1 非流式问答

```bash
curl -X POST "http://localhost:8080/ai/agent/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"帮我推荐南山粤菜，人均80元\",\"city\":\"深圳\",\"scene\":\"recommendation\"}"
```

### 2.2 流式问答（SSE）

```bash
curl -N -X POST "http://localhost:8080/ai/agent/chat/stream" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"推荐科技园附近晚餐\",\"city\":\"深圳\"}"
```

SSE 事件保持：
- `meta`
- `chunk`
- `done`
- `error`

### 2.3 清理会话

```bash
curl -X DELETE "http://localhost:8080/ai/agent/session?conversationId=demo-conversation-001"
```

### 2.4 重建知识索引

```bash
curl -X POST "http://localhost:8080/ai/agent/knowledge/rebuild"
```
