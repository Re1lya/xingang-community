import { createBrowserRouter } from "react-router-dom";
import { App } from "../App";
import { AiChatPage } from "../pages/AiChatPage";
import { AiSessionPage } from "../pages/AiSessionPage";
import { ErrorPage } from "../pages/ErrorPage";
import { HomePage } from "../pages/HomePage";
import { LoginPage } from "../pages/LoginPage";
import { SeckillResultPage } from "../pages/SeckillResultPage";
import { ShopDetailPage } from "../pages/ShopDetailPage";
import { ShopListPage } from "../pages/ShopListPage";
import { VoucherListPage } from "../pages/VoucherListPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "login", element: <LoginPage /> },
      { path: "shops", element: <ShopListPage /> },
      { path: "shops/:shopId", element: <ShopDetailPage /> },
      { path: "vouchers/:shopId", element: <VoucherListPage /> },
      { path: "seckill/result/:orderId", element: <SeckillResultPage /> },
      { path: "ai/chat", element: <AiChatPage /> },
      { path: "ai/session", element: <AiSessionPage /> },
      { path: "error", element: <ErrorPage /> }
    ]
  }
]);
