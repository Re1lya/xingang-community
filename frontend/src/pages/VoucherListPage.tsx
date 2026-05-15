import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { VoucherCard } from "../components/voucher/VoucherCard";
import { StateBlock } from "../components/common/StateBlock";
import { ApiErrorBlock } from "../components/common/ApiErrorBlock";
import { getShopVouchers } from "../services/voucherApi";
import { seckillVoucher } from "../services/seckillApi";

export function VoucherListPage() {
  const { shopId = "" } = useParams();
  const navigate = useNavigate();
  const vouchersQuery = useQuery({
    queryKey: ["vouchers", shopId],
    queryFn: () => getShopVouchers(shopId),
    enabled: Boolean(shopId)
  });
  const seckillMutation = useMutation({
    mutationFn: seckillVoucher,
    onSuccess: (data) => {
      const orderId = typeof data === "object" ? data.orderId : data;
      navigate(`/seckill/result/${orderId ?? "pending"}`);
    }
  });

  return (
    <div className="page-stack">
      <section className="page-title-row">
        <div>
          <p className="eyebrow">优惠券</p>
          <h1>商户可用券</h1>
        </div>
      </section>

      {vouchersQuery.isLoading ? (
        <div className="skeleton-list" aria-label="优惠券加载中">
          <span />
          <span />
          <span />
        </div>
      ) : vouchersQuery.isError ? (
        <ApiErrorBlock title="优惠券加载失败" error={vouchersQuery.error} />
      ) : vouchersQuery.data?.length ? (
        <div className="content-list">
          {vouchersQuery.data.map((voucher) => (
            <VoucherCard
              key={voucher.id}
              voucher={voucher}
              loading={seckillMutation.isPending}
              onSeckill={(voucherId) => seckillMutation.mutate(voucherId)}
            />
          ))}
        </div>
      ) : (
        <StateBlock title="暂无优惠券" description="当前商户还没有可展示的优惠券。" />
      )}
      {seckillMutation.isError ? <ApiErrorBlock title="抢券失败" error={seckillMutation.error} /> : null}
    </div>
  );
}
