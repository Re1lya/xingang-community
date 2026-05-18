import { useRef, useState } from "react";
import { streamChat } from "../services/aiApi";
import type { AgentChatRequest, SseChatEvent } from "../types/ai";

export function useSseChat() {
  const [isStreaming, setIsStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  async function send(payload: AgentChatRequest, onEvent: (event: SseChatEvent) => void) {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setIsStreaming(true);
    try {
      await streamChat(payload, onEvent, controller.signal);
    } finally {
      setIsStreaming(false);
    }
  }

  function stop() {
    abortRef.current?.abort();
    setIsStreaming(false);
  }

  return {
    isStreaming,
    send,
    stop
  };
}
