import { createSlice } from "@reduxjs/toolkit";

const cartSlice = createSlice({
  name: "cart",
  initialState: {
    products: {},
  },
  reducers: {
    addProduct: (state, action) => {
      state.products[action.payload.productId] = {
        quantity: 1,
        price: action.payload.price,
        stock: action.payload.stock,
        name: action.payload.name,
        category: action.payload.category,
        imgName: action.payload.imgName
      };
    },
    clearCart: (state) => {
      state.products = {};
    },
    increaseProductCount: (state, action) => {
      if (
        state.products[action.payload.productId].quantity ===
        state.products[action.payload.productId].stock
      )
        return;
      state.products[action.payload.productId].quantity++;
    },
    decreaseProductCount: (state, action) => {
      const product = state.products[action.payload.productId];
      if (!product) return;

      product.quantity--;
      if (product.quantity === 0) {
        delete state.products[action.payload.productId];
      }
    },
  },
});
export const { addProduct, increaseProductCount, decreaseProductCount, clearCart } =
  cartSlice.actions;
export default cartSlice.reducer;
