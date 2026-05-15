import { Clock, Zap } from "lucide-react";
import type { Voucher } from "../../types/voucher";
import { formatFen, formatTimeRange } from "../../utils/format";

type VoucherCardProps = {
  voucher: Voucher;
  onSeckill?: (voucherId: number) => void;
  loading?: boolean;
};

export function VoucherCard({ voucher, onSeckill, loading }: VoucherCardProps) {
  const isSeckill = voucher.stock != null || voucher.beginTime || voucher.endTime;

  return (
    <article className="voucher-card">
      <div className="voucher-card__value">
        <strong>{formatFen(voucher.actualValue ?? voucher.discountAmount)}</strong>
        <span>{voucher.payValue ? `${formatFen(voucher.payValue)} 起用` : "规则见详情"}</span>
      </div>
      <div className="voucher-card__body">
        <h3>{voucher.title}</h3>
        <p>{voucher.subTitle ?? voucher.rules ?? "以商户实际可用规则为准"}</p>
        {isSeckill ? (
          <p className="voucher-card__time">
            <Clock size={14} aria-hidden="true" />
            {formatTimeRange(voucher.beginTime, voucher.endTime)}
          </p>
        ) : null}
      </div>
      {isSeckill && onSeckill ? (
        <button className="button button--hot" disabled={loading || voucher.stock === 0} onClick={() => onSeckill(voucher.id)}>
          <Zap size={16} aria-hidden="true" />
          {loading ? "处理中" : voucher.stock === 0 ? "已抢光" : "立即抢"}
        </button>
      ) : null}
    </article>
  );
}
