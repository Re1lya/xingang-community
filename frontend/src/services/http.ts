import axios, { AxiosError } from "axios";
import type { ApiErrorPayload, ApiResult } from "../types/api";
import { useAuthStore } from "../stores/authStore";

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 15000
});

export function buildAuthHeaders(): Record<string, string> {
  const { token, user } = useAuthStore.getState();
  const headers: Record<string, string> = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
    headers["X-Token"] = token;
  }
  if (user?.userId || user?.id) {
    headers["X-User-Id"] = String(user.userId ?? user.id);
  }
  return headers;
}

http.interceptors.request.use((config) => {
  Object.assign(config.headers, buildAuthHeaders());
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>;
    if (body && typeof body === "object" && "success" in body && !body.success) {
      const error: ApiErrorPayload = {
        code: body.code,
        message: body.message,
        traceId: body.traceId,
        status: response.status
      };
      return Promise.reject(error);
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const payload: ApiErrorPayload = {
      code: error.response?.data?.code ?? String(error.response?.status ?? "NETWORK_ERROR"),
      message: error.response?.data?.message ?? error.message,
      traceId: error.response?.data?.traceId,
      status: error.response?.status
    };
    return Promise.reject(payload);
  }
);

export async function unwrap<T>(request: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await request;
  return response.data.data;
}
