import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { SlidersHorizontal } from "lucide-react";
import { ShopCard } from "../components/shop/ShopCard";
import { StateBlock } from "../components/common/StateBlock";
import { ApiErrorBlock } from "../components/common/ApiErrorBlock";
import { DOCUMENTED_DEMO_CATEGORIES } from "../constants/shopCategories";
import { getShopsByType } from "../services/shopApi";
import { useLocationStore } from "../stores/locationStore";

export function ShopListPage() {
  const [typeId, setTypeId] = useState(1);
  const { longitude, latitude } = useLocationStore();

  const shopsQuery = useQuery({
    queryKey: ["shops", typeId, longitude, latitude],
    queryFn: () =>
      getShopsByType({
        typeId,
        current: 1,
        pageSize: 10,
        x: longitude,
        y: latitude
      })
  });

  return (
    <div className="page-stack">
      <section className="page-title-row">
        <div>
          <p className="eyebrow">商户</p>
          <h1>附近商户</h1>
        </div>
        <button className="icon-button" type="button" aria-label="筛选">
          <SlidersHorizontal size={18} aria-hidden="true" />
        </button>
      </section>

      <div className="segmented-control" role="tablist" aria-label="商户分类">
        {DOCUMENTED_DEMO_CATEGORIES.map((category) => (
          <button
            key={category.id}
            className={category.id === typeId ? "is-active" : ""}
            type="button"
            onClick={() => setTypeId(category.id)}
          >
            {category.label}
          </button>
        ))}
      </div>

      {shopsQuery.isLoading ? (
        <div className="skeleton-list" aria-label="商户加载中">
          <span />
          <span />
          <span />
        </div>
      ) : shopsQuery.isError ? (
        <ApiErrorBlock title="商户暂时加载失败" error={shopsQuery.error} />
      ) : shopsQuery.data?.length ? (
        <div className="content-list">
          {shopsQuery.data.map((shop) => (
            <ShopCard key={shop.id} shop={shop} />
          ))}
        </div>
      ) : (
        <StateBlock title="当前分类暂无商户" description="可以切换分类或稍后重新加载。" />
      )}
    </div>
  );
}
