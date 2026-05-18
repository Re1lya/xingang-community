export const DOCUMENTED_DEMO_CATEGORIES = [
  { id: 1, label: "美食" },
  { id: 2, label: "咖啡" },
  { id: 3, label: "休闲" }
] as const;

export type DocumentedDemoCategoryId = (typeof DOCUMENTED_DEMO_CATEGORIES)[number]["id"];
