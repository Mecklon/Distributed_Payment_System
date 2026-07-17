import { configureStore } from "@reduxjs/toolkit";
import authSlice from "./AuthSlice";
import cartSlice from "./cartSlice";

export const store = configureStore({
  reducer: {
    auth: authSlice,
    cart: cartSlice,
  },
});
