import { http, unwrap } from "./http";
import type { SeckillOrderResponse } from "../types/voucher";

export function seckillVoucher(voucherId: string | number) {
  return unwrap<SeckillOrderResponse | string | number>(
    http.post(`/voucher-order/seckill/${voucherId}`)
  );
}
