package com.mecklon.order.dtos.types;

public enum WebSocketEventType {
    ORDER_CREATED,
    PAYMENT_CONFIRMED,
    PAYMENT_EXPIRED,
    FAILED_ORDER_ID_CREATION,
    PRODUCTS_RESERVED, FAILED_PRODUCT_RESERVATION
}
