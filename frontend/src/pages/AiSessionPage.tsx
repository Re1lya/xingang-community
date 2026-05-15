import { useMutation } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { StateBlock } from "../components/common/StateBlock";
import { TraceNote } from "../components/common/TraceNote";
import { clearSession } from "../services/aiApi";
import { useAiSessionStore } from "../stores/aiSessionStore";

export function AiSessionPage() {
  const { conversationId, traceId, clearSession: clearLocalSession } = useAiSessionStore();
  const mutation = useMutation({
    mutationFn: clearSession,
    onSuccess: () => clearLocalSession()
  });

  return (
    <div className="page-stack">
      <section className="page-title-row">
        <div>
          <p className="eyebrow">AI 会话</p>
          <h1>会话状态</h1>
        </div>
      </section>
      <StateBlock
        title={conversationId ? "当前会话已建立" : "暂无会话"}
        description={conversationId ? `conversationId: ${conversationId}` : "发送一次 AI 问答后会记录会话标识。"}
        action={
          <button className="button button--danger" type="button" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            <Trash2 size={16} aria-hidden="true" />
            {mutation.isPending ? "清理中" : "清理会话"}
          </button>
        }
      />
      <TraceNote traceId={traceId} />
    </div>
  );
}
