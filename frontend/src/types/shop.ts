export type ShopType = {
  id: number;
  name: string;
  icon?: string;
  sort?: number;
};

export type Shop = {
  id: number;
  name: string;
  typeId?: number;
  images?: string;
  area?: string;
  address?: string;
  x?: number;
  y?: number;
  avgPrice?: number;
  sold?: number;
  comments?: number;
  score?: number;
  openHours?: string;
  distance?: number;
};
