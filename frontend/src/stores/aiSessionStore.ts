import { create } from "zustand";
import { persist } from "zustand/middleware";

type AiSessionState = {
  conversationId?: string;
  traceId?: string;
  setSession: (session: { conversationId?: string; traceId?: string }) => void;
  clearSession: () => void;
};

export const useAiSessionStore = create<AiSessionState>()(
  persist(
    (set) => ({
      setSession: (session) => set(session),
      clearSession: () => set({ conversationId: undefined, traceId: undefined })
    }),
    {
      name: "xingang-ai-session"
    }
  )
);
