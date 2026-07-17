import React, { useEffect, useState } from 'react';
import NavBar from './components/NavBar';
import useGetFetch from './hooks/useGetFetch';
import Image from './hooks/Image';
import placeHolder from './assets/productPlaceholder.jpg';

function formatStatus(status) {
  if (!status) return 'Unknown';
  return String(status)
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatSagaStatus(status) {
  const map = {
    PRODUCT_RESERVED_EVENT: 'Received product reserved event from product service',
    CREATED_PAYMENT_EVENT: 'Successfully created and received razorpay order id from payment service',
    FAILED_PRODUCT_RESERVED_EVENT: 'Received failed product reservation event from product service',
    FAILED_CREATE_ORDER_ID_EVENT: 'Received failed to created razorpay order id from payment service',
    RELEASED_PRODUCT_EVENT: 'Received released product event from product service',
    EXPIRED_PAYMENT_EVENT: 'Received payment expired event from payment service',
    REFUNDED_EVENT: 'Received payment refunded event from payment service',
    REFUND_FAILED_EVENT: 'Received payment refund failed event from payment service',
    CREATE_PAYMENT_COMMAND: 'Sent create razorpay order id command to payment service',
    RELEASE_PRODUCT_ORDER_ID_CREATION_FAILED_COMMAND: 'Sent release product command to product service after failure to create razorpay order id',
    RELEASE_PRODUCT_PAYMENT_EXPIRED_COMMAND: 'Sent release product command to product serice after payment time expired',
    RESERVE_PRODUCT_COMMAND: 'Sent reserve product command to product service',
    PAYMENT_SUCCESSFUL_EVENT: 'Received payment successfull event from payment service and marked as booked',
    CREATED: 'Order created',
  };

  return map[status] || formatStatus(status);
}

function formatTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function OrderHistory() {
  const { state: orders = [], loading, error, fetch } = useGetFetch([]);
  const [expandedOrderId, setExpandedOrderId] = useState(null);

  useEffect(() => {
    fetch(`${import.meta.env.VITE_ORDER_URL}getHistory`);
  }, []);

  useEffect(() => {
    if (orders.length > 0 && !expandedOrderId) {
      setExpandedOrderId(orders[0].orderId);
    }
  }, [orders, expandedOrderId]);

  return (
    <div className="min-h-screen overflow-y-auto bg-slate-50">
      <NavBar />
      <div className="mx-auto max-w-7xl px-6 py-8">
        <div className="border border-slate-300 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-200 pb-4">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Order History</h1>
              <p className="mt-1 text-sm text-slate-600">Your recent orders and their saga timeline.</p>
            </div>
          </div>

          {loading && (
            <div className="mt-6 border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
              Loading your orders...
            </div>
          )}

          {error && (
            <div className="mt-6 border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              Unable to load order history right now.
            </div>
          )}

          {!loading && !error && orders.length === 0 && (
            <div className="mt-6 border border-dashed border-slate-300 p-6 text-center text-slate-600">
              No orders found yet.
            </div>
          )}

          <div className="mt-6 space-y-4">
            {orders.map((order) => {
              const isExpanded = expandedOrderId === order.orderId;
              return (
                <div key={order.orderId} className="overflow-hidden border border-slate-200 bg-white shadow-sm">
                  <button
                    type="button"
                    onClick={() => setExpandedOrderId(isExpanded ? null : order.orderId)}
                    className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                  >
                    <div>
                      <div className="text-lg font-semibold text-slate-900">Order ID: {order.orderId}</div>
                      <div className="mt-1 flex gap-6">
                        <div className="text-sm font-semibold text-slate-900">Status: {formatStatus(order.status)}</div>
                        <div className="text-xs text-slate-500">Created: {formatTime(order.createdAt)}</div>
                      </div>
                    </div>
                    <span className="border border-slate-300 bg-slate-50 px-3 py-1 text-sm font-semibold text-slate-700">
                      {isExpanded ? 'Hide details' : 'View details'}
                    </span>
                  </button>

                  {isExpanded && (
                    <div className="border-t border-slate-200 bg-slate-50 px-5 py-5">
                      <div className="flex gap-6">
                        <div className="flex-1">
                          <h2 className="text-base font-semibold text-slate-900">Products</h2>
                          <div className="mt-3 space-y-3">
                            {(order.products || []).map((product, index) => (
                              <div key={`${order.orderId}-${product.productId || index}`} className="flex items-center gap-4 border border-slate-200 bg-white p-3">
                                <div className="h-16 w-16 shrink-0 overflow-hidden border border-slate-200 bg-slate-100">
                                  <Image path={product.imgName} fallback={placeHolder} className="h-full w-full object-cover" />
                                </div>
                                <div className="flex-1">
                                  <div className="text-sm font-semibold text-slate-900">{product.name || product.productId}</div>
                                  <div className="text-sm text-slate-600">{product.category || 'Uncategorized'}</div>
                                </div>
                                <div className="text-sm text-slate-600">Qty: {product.quantity}</div>
                                <div className="text-sm font-semibold text-blue-950">₹ {product.price}</div>
                              </div>
                            ))}
                          </div>
                        </div>

                        <div className="flex-1">
                          <h2 className="text-base font-semibold text-slate-900">Saga timeline</h2>
                          <div className="mt-3 space-y-3">
                            {(order.history || []).map((event, index) => (
                              <div key={`${order.orderId}-${index}`} className="flex gap-3 border border-slate-200 bg-white p-3">
                                <div className={`mt-1 h-3 w-3 shrink-0 rounded-full ${event.isCompensationEvent ? 'bg-rose-500' : 'bg-emerald-500'}`} />
                                <div className="flex-1">
                                  <div className="text-sm font-semibold text-slate-900">{formatSagaStatus(event.status)}</div>
                                  <div className="text-xs text-slate-500">{formatTime(event.time)}</div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

export default OrderHistory;
