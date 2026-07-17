package com.mecklon.order.models.types;

public enum OrderStatus {
    RESERVED,
    CREATED,
    FAILED_ORDERID_CREATION,
    FAILED_PRODUCT_RESERVATION,
    PAYMENT_PENDING,
    BOOKED,
    PAYMENT_EXPIRED,
    REFUNDED,
    REFUND_FAILED

}
