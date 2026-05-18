import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronRight, MapPin, Star } from "lucide-react";
import { StateBlock } from "../components/common/StateBlock";
import { ApiErrorBlock } from "../components/common/ApiErrorBlock";
import { VoucherCard } from "../components/voucher/VoucherCard";
import { getShopDetail } from "../services/shopApi";
import { getShopVouchers } from "../services/voucherApi";
import { seckillVoucher } from "../services/seckillApi";
import { formatFen, formatScore } from "../utils/format";

export function ShopDetailPage() {
  const { shopId = "" } = useParams();
  const navigate = useNavigate();
  const shopQuery = useQuery({
    queryKey: ["shop", shopId],
    queryFn: () => getShopDetail(shopId),
    enabled: Boolean(shopId)
  });
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

  if (shopQuery.isLoading) {
    return <StateBlock title="商户加载中" description="正在读取商户详情。" />;
  }

  if (shopQuery.isError || !shopQuery.data) {
    return <StateBlock title="商户不存在或暂时不可用" description="请返回商户列表重新选择。" />;
  }

  const shop = shopQuery.data;

  return (
    <div className="page-stack">
      <section className="detail-hero">
        <div className="detail-hero__image" aria-hidden="true">
          {shop.name.slice(0, 1)}
        </div>
        <div className="detail-hero__body">
          <h1>{shop.name}</h1>
          <p>
            <Star size={15} aria-hidden="true" />
            {formatScore(shop.score)}
            <span>{formatFen(shop.avgPrice)} / 人</span>
          </p>
          <p>
            <MapPin size={15} aria-hidden="true" />
            {shop.area ?? "社区周边"} · {shop.address ?? "地址待确认"}
          </p>
        </div>
      </section>

      <section className="section-block">
        <div className="section-heading">
          <div>
            <p className="eyebrow">优惠券</p>
            <h2>可用券</h2>
          </div>
          <Link className="text-link" to={`/vouchers/${shopId}`}>
            全部
            <ChevronRight size={15} aria-hidden="true" />
          </Link>
        </div>
        {vouchersQuery.isLoading ? (
          <div className="skeleton-list" aria-label="优惠券加载中">
            <span />
            <span />
          </div>
        ) : vouchersQuery.isError ? (
          <ApiErrorBlock title="优惠券加载失败" error={vouchersQuery.error} />
        ) : vouchersQuery.data?.length ? (
          <div className="content-list">
            {vouchersQuery.data.slice(0, 3).map((voucher) => (
              <VoucherCard
                key={voucher.id}
                voucher={voucher}
                loading={seckillMutation.isPending}
                onSeckill={(voucherId) => seckillMutation.mutate(voucherId)}
              />
            ))}
          </div>
        ) : (
          <StateBlock title="暂无优惠券" description="商户还没有配置可领取的优惠券。" />
        )}
        {seckillMutation.isError ? (
          <ApiErrorBlock title="抢券失败" error={seckillMutation.error} />
        ) : null}
      </section>
    </div>
  );
}
