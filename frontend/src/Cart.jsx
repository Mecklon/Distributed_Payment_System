import React from "react";
import { useDispatch, useSelector } from "react-redux";
import NavBar from "./components/NavBar";
import Image from "./hooks/Image";
import placeHolder from "./assets/productPlaceholder.jpg";
import { increaseProductCount, decreaseProductCount } from "./store/cartSlice";
import { FiPlus, FiMinus } from "react-icons/fi";
import { Link } from "react-router-dom";

function buildOrderRequest(cartProducts){
  const rows = Object.entries(cartProducts);
  const products = rows.map(([id, item]) => ({
    productId: id,
    name: item.name,
    category: item.category,
    price: item.price,
    imgName: item.imgName,
    quantity: item.quantity
  }));
  const checkoutSessionId = crypto.randomUUID();
  return { checkoutSessionId, products }
}

function Cart() {
  const dispatch = useDispatch();
  const cartProducts = useSelector((store) => store.cart.products || {});

  const rows = Object.entries(cartProducts);

  const total = rows.reduce((acc, [id, item]) => acc + (item.price || 0) * (item.quantity || 0), 0);

  return (
    <div className="min-h-screen bg-slate-50">
      <NavBar />
      <div className="mx-auto max-w-7xl px-6 py-8">
        <div className="border border-slate-300 bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-bold text-slate-900">Your Cart</h1>

          {rows.length === 0 ? (
            <div className="mt-6 text-blue-900 text-lg">Your cart is empty.</div>
          ) : (
            <div className="mt-6 space-y-4">
              {rows.map(([id, item]) => (
                <div key={id} className="flex items-center gap-4 border border-slate-200 bg-white p-4">
                  <div className="w-28 shrink-0 overflow-hidden border border-slate-200 bg-slate-100">
                    <Image path={item.imgName} fallback={placeHolder} className="h-28 w-28 object-cover" />
                  </div>

                  <div className="flex flex-1 flex-col gap-1">
                    <div className="flex items-center justify-between">
                      <div className="text-lg font-semibold text-slate-900">{item.name}</div>
                      <div className="text-lg font-semibold text-blue-950">₹ {item.price}</div>
                    </div>
                    <div className="text-sm text-slate-600">{item.stock} in stock</div>
                    <div className="mt-2 text-sm text-slate-700">{item.description}</div>
                  </div>

                  <div className="flex flex-col items-end gap-2">
                    <div className="flex items-center justify-between border border-blue-950 bg-blue-950 px-3 text-sm text-white">
                      <button
                        onClick={() => dispatch(decreaseProductCount({ productId: id }))}
                        className="rounded-full aspect-square bg-blue-950 px-3 py-1 font-semibold text-white transition hover:bg-blue-900"
                      >
                        <FiMinus className="text-2xl" />
                      </button>
                      <div className="px-3 text-base font-semibold">{item.quantity}</div>
                      <button
                        onClick={() => dispatch(increaseProductCount({ productId: id }))}
                        className="rounded-full aspect-square bg-blue-950 px-3 py-1 font-semibold text-white transition hover:bg-blue-900"
                      >
                        <FiPlus className="text-2xl" />
                      </button>
                    </div>
                    <div className="text-sm text-slate-600">Subtotal: ₹ {item.price * item.quantity}</div>
                  </div>
                </div>
              ))}

              <div className="mt-4 flex items-center justify-between border border-slate-200 bg-white p-4">
                <div className="text-xl font-semibold text-slate-900">Total</div>
                <div className="flex items-center gap-4">
                  <div className="text-2xl font-bold text-blue-950">₹ {total}</div>
                  <Link to="/payment" state={buildOrderRequest(cartProducts)} className="border border-blue-950 bg-blue-950 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-800">
                    Book
                  </Link>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Cart;
