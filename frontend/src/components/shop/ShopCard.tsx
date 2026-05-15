import { Link } from "react-router-dom";
import { MapPin, Star } from "lucide-react";
import type { Shop } from "../../types/shop";
import { formatDistance, formatFen, formatScore } from "../../utils/format";

type ShopCardProps = {
  shop: Shop;
};

export function ShopCard({ shop }: ShopCardProps) {
  return (
    <article className="shop-card">
      <div className="shop-card__image" aria-hidden="true">
        {shop.name?.slice(0, 1) ?? "店"}
      </div>
      <div className="shop-card__body">
        <div className="shop-card__title-row">
          <h3>{shop.name}</h3>
          <span>{formatDistance(shop.distance)}</span>
        </div>
        <p className="shop-card__meta">
          <Star size={14} aria-hidden="true" />
          {formatScore(shop.score)}
          <span>{formatFen(shop.avgPrice)} / 人</span>
        </p>
        <p className="shop-card__address">
          <MapPin size={14} aria-hidden="true" />
          {shop.area ?? "社区周边"} · {shop.address ?? "地址待确认"}
        </p>
        <Link className="text-link" to={`/shops/${shop.id}`}>
          查看详情
        </Link>
      </div>
    </article>
  );
}
