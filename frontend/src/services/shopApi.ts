import { http, unwrap } from "./http";
import type { Shop } from "../types/shop";

export type ShopListParams = {
  typeId: number;
  current?: number;
  pageSize?: number;
  x?: number;
  y?: number;
};

export function getShopsByType(params: ShopListParams) {
  return unwrap<Shop[]>(http.get("/shop/of/type", { params }));
}

export function getShopDetail(shopId: string | number) {
  return unwrap<Shop>(http.get(`/shop/${shopId}`));
}
