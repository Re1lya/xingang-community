import type { ReactNode } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import { Bot, Home, MapPin, MessageCircle, Ticket, UserRound } from "lucide-react";
import { useAuthStore } from "../../stores/authStore";
import { useLocationStore } from "../../stores/locationStore";

type AppShellProps = {
  children: ReactNode;
};

const navItems = [
  { to: "/", label: "首页", icon: Home },
  { to: "/shops", label: "商户", icon: MapPin },
  { to: "/ai/chat", label: "AI", icon: MessageCircle }
];

export function AppShell({ children }: AppShellProps) {
  const { pathname } = useLocation();
  const city = useLocationStore((state) => state.city);
  const user = useAuthStore((state) => state.user);
  const isLoginPage = pathname === "/login";

  return (
    <div className="app-shell">
      {!isLoginPage ? (
        <header className="app-header">
          <Link to="/" className="brand-mark" aria-label="返回首页">
            <span className="brand-mark__icon">
              <Ticket size={18} aria-hidden="true" />
            </span>
            <span>新港社区</span>
          </Link>
          <div className="header-location">
            <MapPin size={15} aria-hidden="true" />
            <span>{city}</span>
          </div>
          <Link to={user ? "/ai/session" : "/login"} className="user-chip">
            {user ? <Bot size={15} aria-hidden="true" /> : <UserRound size={15} aria-hidden="true" />}
            <span>{user?.nickName ?? "登录"}</span>
          </Link>
        </header>
      ) : null}
      <main className={isLoginPage ? "app-main app-main--plain" : "app-main"}>{children}</main>
      {!isLoginPage ? (
        <nav className="bottom-nav" aria-label="主要导航">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink key={item.to} to={item.to} className="bottom-nav__item">
                <Icon size={20} aria-hidden="true" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      ) : null}
    </div>
  );
}
