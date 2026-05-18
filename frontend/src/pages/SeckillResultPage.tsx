import { Link, useParams } from "react-router-dom";
import { CheckCircle2, Clock3 } from "lucide-react";

export function SeckillResultPage() {
  const { orderId } = useParams();
  const isPending = !orderId || orderId === "pending";

  return (
    <div className="page-stack">
      <section className="result-panel">
        {isPending ? (
          <Clock3 size={40} aria-hidden="true" />
        ) : (
          <CheckCircle2 size={40} aria-hidden="true" />
        )}
        <h1>{isPending ? "已提交抢券请求" : "已进入抢券队列"}</h1>
        <p>{isPending ? "后端未返回明确订单号，请稍后在订单能力补齐后查询。" : `订单号：${orderId}`}</p>
        <div className="button-row">
          <Link className="button button--primary" to="/">
            返回首页
          </Link>
          <Link className="button button--ghost" to="/shops">
            继续浏览
          </Link>
        </div>
      </section>
    </div>
  );
}
