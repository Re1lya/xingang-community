import type { ApiErrorPayload } from "../../types/api";
import { StateBlock } from "./StateBlock";

type ApiErrorBlockProps = {
  title: string;
  error?: unknown;
};

export function ApiErrorBlock({ title, error }: ApiErrorBlockProps) {
  const payload = error as ApiErrorPayload | undefined;
  const details = [payload?.message, payload?.traceId ? `traceId: ${payload.traceId}` : undefined]
    .filter(Boolean)
    .join("\n");

  return <StateBlock title={title} description={details || "请稍后重试。"} />;
}
