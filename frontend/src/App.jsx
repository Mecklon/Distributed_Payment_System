import { BrowserRouter, Link, Route, Routes, Navigate } from "react-router-dom";
import Login from "./Login";
import Signup from "./Signup";
import { useSelector } from "react-redux";
import usePostFetch from "./hooks/usePostFetch";
import { useDispatch } from "react-redux";
import { useState, useEffect } from "react";
import { clearAuth, setAuth } from "./store/authSlice";
import Products from "./Products";
import orderHistory from "./OrderHistory";
import Cart from "./Cart";
import rolling from "./assets/rolling2.svg";
import OrderHistory from "./OrderHistory";
import WebSocketProvider from "./WebSocketProvider"
import Payment from "./Payment";
function App() {
  const auth = useSelector((store) => store.auth);
  const { fetch, loading } = usePostFetch();
  const dispatch = useDispatch();
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    const autoLogin = async () => {
      if (!auth.token || authChecked) {
        setAuthChecked(true);
        return;
      }

      try {
        const data = await fetch(`${import.meta.env.VITE_AUTH_URL}autoLogin`);
        data.token=auth.token
        dispatch(setAuth(data));
      } finally {
        setAuthChecked(true);
      }
    };

    autoLogin();
  }, [auth.token]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <img src={rolling} />
      </div>
    );
  }

  if (auth.username) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/products" element={<Products />}></Route>
          <Route path="/cart" element={<Cart />}></Route>
          <Route path="/orderHistory" element={<OrderHistory />}></Route>
          <Route path="/payment" element={<WebSocketProvider><Payment/></WebSocketProvider>}></Route>
          <Route path="*" element={<Navigate to="/products" />}></Route>
        </Routes>
      </BrowserRouter>
    );
  }

  return (
    <>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />}></Route>
          <Route path="/signup" element={<Signup />}></Route>
          <Route path="*" element={<Navigate to="/login" />}></Route>
        </Routes>
      </BrowserRouter>
    </>
  );
}

export default App;
