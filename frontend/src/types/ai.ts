export type AgentChatRequest = {
  message: string;
  conversationId?: string;
  city?: string;
  longitude?: number;
  latitude?: number;
  scene?: "recommendation" | "support" | "general" | string;
};

export type AgentToolTrace = {
  toolName?: string;
  status?: string;
  summary?: string;
  traceId?: string;
  [key: string]: unknown;
};

export type RetrievalHit = {
  title?: string;
  content?: string;
  score?: number;
  source?: string;
};

export type AgentChatResponse = {
  answer?: string;
  conversationId?: string;
  traceId?: string;
  plan?: unknown;
  toolTrace?: AgentToolTrace[];
  retrievalHits?: RetrievalHit[];
};

export type SseChatEvent =
  | { type: "meta"; data: unknown }
  | { type: "chunk"; data: string }
  | { type: "done"; data: unknown }
  | { type: "error"; data: unknown };
