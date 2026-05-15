import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Bot, Send, Sparkles } from "lucide-react";
import { ChatBubble } from "../components/ai/ChatBubble";
import { chat } from "../services/aiApi";
import { useSseChat } from "../hooks/useSseChat";
import { useAiSessionStore } from "../stores/aiSessionStore";
import { useLocationStore } from "../stores/locationStore";
import type { AgentChatResponse, AgentChatRequest, SseChatEvent } from "../types/ai";

type Message = {
  role: "user" | "assistant" | "system";
  content: string;
};

type ChatMode = "normal" | "stream";

export function AiChatPage() {
  const [input, setInput] = useState("附近有没有适合两个人、人均80以内的餐厅？");
  const [mode, setMode] = useState<ChatMode>("normal");
  const [messages, setMessages] = useState<Message[]>([
    { role: "system", content: "AI 回答只展示后端返回的事实和轨迹。" }
  ]);
  const { conversationId, setSession } = useAiSessionStore();
  const location = useLocationStore();
  const sseChat = useSseChat();

  const mutation = useMutation({
    mutationFn: chat,
    onSuccess: (response: AgentChatResponse) => {
      setSession({ conversationId: response.conversationId, traceId: response.traceId });
      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          content: response.answer || "后端暂未返回 answer 字段。"
        }
      ]);
    },
    onError: (error) => {
      setMessages((current) => [
        ...current,
        { role: "assistant", content: formatErrorMessage("普通问答请求失败", error) }
      ]);
    }
  });

  const isBusy = mutation.isPending || sseChat.isStreaming;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = input.trim();
    if (!message) return;
    setMessages((current) => [...current, { role: "user", content: message }]);
    setInput("");
    const payload: AgentChatRequest = {
      message,
      conversationId,
      city: location.city,
      longitude: location.longitude,
      latitude: location.latitude,
      scene: "recommendation"
    };
    if (mode === "stream") {
      void sseChat.send(payload, handleStreamEvent);
      return;
    }
    mutation.mutate(payload);
  }

  function handleStreamEvent(event: SseChatEvent) {
    if (event.type === "meta" || event.type === "done") {
      const session = readSessionEvent(event.data);
      if (session.conversationId || session.traceId) setSession(session);
    }
    if (event.type === "chunk") {
      appendToLastAssistant(event.data);
    }
    if (event.type === "error") {
      appendToLastAssistant(formatErrorMessage("流式问答请求失败", event.data));
    }
    if (event.type === "done") {
      ensureLastAssistantHasContent("流式回答已结束，后端未返回文本 chunk。");
    }
  }

  function appendToLastAssistant(chunk: string) {
    setMessages((current) => {
      const next = [...current];
      const last = next[next.length - 1];
      if (last?.role === "assistant") {
        next[next.length - 1] = { ...last, content: `${last.content}${chunk}` };
        return next;
      }
      return [...next, { role: "assistant", content: chunk }];
    });
  }

  function ensureLastAssistantHasContent(fallback: string) {
    setMessages((current) => {
      const next = [...current];
      const last = next[next.length - 1];
      if (last?.role === "assistant" && !last.content.trim()) {
        next[next.length - 1] = { ...last, content: fallback };
        return next;
      }
      return [...next, { role: "assistant", content: fallback }];
    });
  }

  return (
    <div className="chat-page">
      <section className="chat-header">
        <div>
          <p className="eyebrow">AI 客服</p>
          <h1>智能推荐</h1>
        </div>
        <span className="status-pill">
          <Sparkles size={14} aria-hidden="true" />
          {mode === "stream" ? "Streaming" : "Normal"}
        </span>
      </section>

      <div className="segmented-control" role="tablist" aria-label="AI 问答模式">
        <button className={mode === "normal" ? "is-active" : ""} type="button" onClick={() => setMode("normal")}>
          普通问答
        </button>
        <button className={mode === "stream" ? "is-active" : ""} type="button" onClick={() => setMode("stream")}>
          流式问答
        </button>
      </div>

      <section className="chat-window" aria-label="聊天消息">
        {messages.map((message, index) => (
          <ChatBubble key={`${message.role}-${index}`} role={message.role}>
            {message.content}
          </ChatBubble>
        ))}
        {isBusy ? (
          <ChatBubble role="assistant">
            <span className="typing-dot" />
            {mode === "stream" ? "正在接收流式响应" : "正在生成"}
          </ChatBubble>
        ) : null}
      </section>

      <form className="chat-composer" onSubmit={handleSubmit}>
        <Bot size={18} aria-hidden="true" />
        <input value={input} onChange={(event) => setInput(event.target.value)} placeholder="输入你的本地生活问题" />
        <button className="icon-button icon-button--filled" type="submit" disabled={isBusy || !input.trim()} aria-label="发送">
          <Send size={18} aria-hidden="true" />
        </button>
      </form>
    </div>
  );
}

function readSessionEvent(data: unknown): { conversationId?: string; traceId?: string } {
  if (!data || typeof data !== "object") return {};
  const record = data as Record<string, unknown>;
  return {
    conversationId: typeof record.conversationId === "string" ? record.conversationId : undefined,
    traceId: typeof record.traceId === "string" ? record.traceId : undefined
  };
}

function formatErrorMessage(prefix: string, error: unknown): string {
  if (!error || typeof error !== "object") return prefix;
  const record = error as Record<string, unknown>;
  const message = typeof record.message === "string" ? record.message : prefix;
  const traceId = typeof record.traceId === "string" ? record.traceId : undefined;
  return traceId ? `${message}\ntraceId: ${traceId}` : message;
}
