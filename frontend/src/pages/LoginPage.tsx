import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useLocation, useNavigate } from "react-router-dom";
import { ArrowRight, Smartphone } from "lucide-react";
import { login } from "../services/userApi";
import { useAuthStore } from "../stores/authStore";
import type { ApiErrorPayload } from "../types/api";

export function LoginPage() {
  const [phone, setPhone] = useState("13800000001");
  const [password, setPassword] = useState("");
  const setLogin = useAuthStore((state) => state.setLogin);
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (user) => {
      setLogin(user);
      navigate(from, { replace: true });
    }
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate({ phone, password });
  }

  const error = mutation.error as ApiErrorPayload | null;

  return (
    <div className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-panel__mark">
          <Smartphone size={24} aria-hidden="true" />
        </div>
        <h1 id="login-title">新港社区</h1>
        <p>登录后进入本地生活服务台。</p>
        <form className="form-stack" onSubmit={handleSubmit}>
          <label>
            手机号
            <input
              value={phone}
              inputMode="tel"
              placeholder="请输入手机号"
              onChange={(event) => setPhone(event.target.value)}
              required
            />
          </label>
          <label>
            密码或验证码
            <input
              value={password}
              placeholder="当前后端允许留空时可直接演示"
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {error ? (
            <div className="inline-error">
              <strong>{error.message}</strong>
              {error.traceId ? <span>traceId: {error.traceId}</span> : null}
            </div>
          ) : null}
          <button className="button button--primary" type="submit" disabled={mutation.isPending || !phone.trim()}>
            {mutation.isPending ? "登录中" : "进入首页"}
            <ArrowRight size={17} aria-hidden="true" />
          </button>
        </form>
      </section>
    </div>
  );
}
