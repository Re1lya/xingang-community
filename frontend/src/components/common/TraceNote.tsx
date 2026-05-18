type TraceNoteProps = {
  traceId?: string;
};

export function TraceNote({ traceId }: TraceNoteProps) {
  if (!traceId) return null;
  return <p className="trace-note">traceId: {traceId}</p>;
}
