import type { ReactNode } from "react";

type ChatBubbleProps = {
  role: "user" | "assistant" | "system";
  children: ReactNode;
};

export function ChatBubble({ role, children }: ChatBubbleProps) {
  return <div className={`chat-bubble chat-bubble--${role}`}>{children}</div>;
}
