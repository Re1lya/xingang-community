export type Voucher = {
  id: number;
  shopId: number;
  title: string;
  subTitle?: string;
  rules?: string;
  payValue?: number;
  actualValue?: number;
  discountAmount?: number;
  stock?: number;
  beginTime?: string;
  endTime?: string;
};

export type SeckillOrderResponse = {
  orderId?: number | string;
};
