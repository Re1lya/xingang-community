export function formatFen(value?: number | null): string {
  if (value == null) return "价格待确认";
  return `¥${(value / 100).toFixed(2)}`;
}

export function formatDistance(value?: number | null): string {
  if (value == null) return "距离待确认";
  if (value < 1000) return `${Math.round(value)}m`;
  return `${(value / 1000).toFixed(1)}km`;
}

export function formatScore(value?: number | null): string {
  if (value == null) return "评分待确认";
  return `${Number(value).toFixed(1)} 分`;
}

export function formatTimeRange(begin?: string, end?: string): string {
  if (!begin && !end) return "活动时间待确认";
  return `${begin ?? "开始时间待确认"} 至 ${end ?? "结束时间待确认"}`;
}
