import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { LoginUser } from "../types/user";

type AuthState = {
  token?: string;
  user?: LoginUser;
  setLogin: (user: LoginUser) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      setLogin: (user) =>
        set({
          user,
          token: user.token
        }),
      logout: () =>
        set({
          user: undefined,
          token: undefined
        })
    }),
    {
      name: "xingang-auth"
    }
  )
);
