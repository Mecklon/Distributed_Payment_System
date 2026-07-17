package com.mecklon.payment;

import com.mecklon.payment.dtos.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "razorpay",
        url = "https://api.razorpay.com"
)
public interface RazorpayClient {

    @PostMapping(
            value = "/v1/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    RazorpayOrderResponse createOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RazorpayOrderRequest request
    );


    @GetMapping("/v1/orders/{orderId}/payments")
    RazorpayPaymentsResponse getPayments(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String orderId
    );

    @PostMapping("/v1/payments/{paymentId}/refund")
    RazorpayRefundResponse refund(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Refund-Idempotency") String idempotencyKey,
            @PathVariable String paymentId,
            @RequestBody RefundRequest request
    );
}