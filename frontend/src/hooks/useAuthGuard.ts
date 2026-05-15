import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "../stores/authStore";

export function useAuthGuard(enabled = true) {
  const token = useAuthStore((state) => state.token);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (enabled && !token) {
      navigate("/login", {
        replace: true,
        state: { from: location.pathname }
      });
    }
  }, [enabled, location.pathname, navigate, token]);

  return Boolean(token);
}
