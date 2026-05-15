import { http, unwrap } from "./http";
import type { LoginRequest, LoginUser } from "../types/user";

export function login(payload: LoginRequest) {
  return unwrap<LoginUser>(http.post("/user/login", payload));
}
