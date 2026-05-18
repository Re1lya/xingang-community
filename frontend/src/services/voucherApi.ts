import { http, unwrap } from "./http";
import type { Voucher } from "../types/voucher";

export function getShopVouchers(shopId: string | number) {
  return unwrap<Voucher[]>(http.get(`/voucher/list/${shopId}`));
}
