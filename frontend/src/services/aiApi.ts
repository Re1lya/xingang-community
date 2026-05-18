import { buildAuthHeaders, http, unwrap } from "./http";
import type { AgentChatRequest, AgentChatResponse, SseChatEvent } from "../types/ai";

export function chat(payload: AgentChatRequest) {
  return unwrap<AgentChatResponse>(http.post("/ai/agent/chat", payload));
}

export function clearSession() {
  return unwrap<{ conversationId?: string; traceId?: string }>(http.delete("/ai/agent/session"));
}

export async function streamChat(
  payload: AgentChatRequest,
  onEvent: (event: SseChatEvent) => void,
  signal?: AbortSignal
) {
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL ?? "/api"}/ai/agent/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...buildAuthHeaders()
    },
    body: JSON.stringify(payload),
    signal
  });

  if (!response.ok || !response.body) {
    onEvent({ type: "error", data: { status: response.status, message: response.statusText } });
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() ?? "";
    for (const rawEvent of events) {
      const parsed = parseSseEvent(rawEvent);
      if (parsed) onEvent(parsed);
    }
  }
}

function parseSseEvent(rawEvent: string): SseChatEvent | null {
  const lines = rawEvent.split(/\r?\n/);
  const eventLine = lines.find((line) => line.startsWith("event:"));
  const dataLines = lines.filter((line) => line.startsWith("data:"));
  const type = eventLine?.replace("event:", "").trim() || "chunk";
  const rawData = dataLines.map((line) => line.replace("data:", "").trim()).join("\n");

  if (type === "chunk") return { type: "chunk", data: rawData };
  if (type === "meta" || type === "done" || type === "error") {
    try {
      return { type, data: rawData ? JSON.parse(rawData) : {} };
    } catch {
      return { type, data: rawData };
    }
  }
  return null;
}
