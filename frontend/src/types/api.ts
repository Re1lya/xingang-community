export type ApiResult<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  traceId: string;
  timestamp: number;
};

export type ApiErrorPayload = {
  code: string;
  message: string;
  traceId?: string;
  status?: number;
};

export type PageResult<T> = {
  records: T[];
  total?: number;
  size?: number;
  current?: number;
};
