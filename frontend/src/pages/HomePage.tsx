import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Bot, ChevronRight, Flame, Search, Store, Ticket } from "lucide-react";
import { ShopCard } from "../components/shop/ShopCard";
import { ApiErrorBlock } from "../components/common/ApiErrorBlock";
import { StateBlock } from "../components/common/StateBlock";
import { DOCUMENTED_DEMO_CATEGORIES } from "../constants/shopCategories";
import { getShopsByType } from "../services/shopApi";
import { useLocationStore } from "../stores/locationStore";

export function HomePage() {
  const { longitude, latitude } = useLocationStore();
  const defaultCategory = DOCUMENTED_DEMO_CATEGORIES[0];
  const shopsQuery = useQuery({
    queryKey: ["home-shops", defaultCategory.id, longitude, latitude],
    queryFn: () =>
      getShopsByType({
        typeId: defaultCategory.id,
        current: 1,
        pageSize: 4,
        x: longitude,
        y: latitude
      })
  });

  return (
    <div className="page-stack">
      <section className="home-search">
        <div className="search-box">
          <Search size={18} aria-hidden="true" />
          <span>搜索商户、优惠券或服务问题</span>
        </div>
        <Link className="button button--dark" to="/ai/chat">
          <Bot size={17} aria-hidden="true" />
          问 AI
        </Link>
      </section>

      <section className="quick-grid" aria-label="快捷入口">
        <Link to="/shops">
          <Store size={22} aria-hidden="true" />
          <span>附近商户</span>
        </Link>
        <Link to="/shops/1">
          <Ticket size={22} aria-hidden="true" />
          <span>优惠券</span>
        </Link>
        <Link to="/vouchers/1">
          <Flame size={22} aria-hidden="true" />
          <span>限时秒杀</span>
        </Link>
        <Link to="/ai/chat">
          <Bot size={22} aria-hidden="true" />
          <span>智能推荐</span>
        </Link>
      </section>

      <section className="section-block">
        <div className="section-heading">
          <div>
            <p className="eyebrow">真实接口 · 演示分类</p>
            <h2>社区附近</h2>
          </div>
          <Link className="text-link" to="/shops">
            全部
            <ChevronRight size={15} aria-hidden="true" />
          </Link>
        </div>
        <p className="section-note">分类来自前端文档演示常量，后续可接后端分类接口。</p>
        {shopsQuery.isLoading ? (
          <div className="skeleton-list" aria-label="首页商户加载中">
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
          <StateBlock title="暂无真实商户数据" description="当前分类没有返回商户，请确认后端种子数据或切换分类。" />
        )}
      </section>
    </div>
  );
}
