export type LoginRequest = {
  phone: string;
  code?: string;
  password?: string;
};

export type LoginUser = {
  token?: string;
  userId?: number;
  id?: number;
  nickName?: string;
  icon?: string;
};
