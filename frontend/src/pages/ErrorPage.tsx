import { Link, useRouteError } from "react-router-dom";
import { AlertTriangle } from "lucide-react";

export function ErrorPage() {
  const error = useRouteError() as { statusText?: string; message?: string } | undefined;

  return (
    <div className="page-stack">
      <section className="result-panel result-panel--error">
        <AlertTriangle size={40} aria-hidden="true" />
        <h1>页面暂时不可用</h1>
        <p>{error?.statusText ?? error?.message ?? "请返回首页后重试。"}</p>
        <Link className="button button--primary" to="/">
          返回首页
        </Link>
      </section>
    </div>
  );
}
