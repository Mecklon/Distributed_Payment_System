package com.mecklon.payment;

import com.mecklon.payment.dtos.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Retry(name = "razorpay")
    @CircuitBreaker(name = "razorpay")
    @TimeLimiter(name = "razorpay")
    public CompletableFuture<RazorpayOrderResponse> createOrder(RazorpayOrderRequest request) {

        String auth = getAuth();

        return CompletableFuture.supplyAsync(() ->
                razorpayClient.createOrder(auth, request)
        );
    }

    @Retry(name = "razorpay")
    @CircuitBreaker(name = "razorpay")
    @TimeLimiter(name = "razorpay")
    public CompletableFuture<RazorpayPaymentsResponse> getPayments(String orderId) {
        String auth = getAuth();

        return CompletableFuture.supplyAsync(() ->
                razorpayClient.getPayments(auth,orderId)
        );
    }

    @Retry(name = "razorpay")
    @CircuitBreaker(name = "razorpay")
    @TimeLimiter(name = "razorpay")
    public CompletableFuture<RazorpayRefundResponse> refund(int amount,String idempotencyKey, String paymentId) {

        String auth = getAuth();
        return CompletableFuture.supplyAsync(() ->
                razorpayClient.refund(auth, paymentId,idempotencyKey, new RefundRequest(amount))
        );
    }

    private String getAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (keyId + ":" + keySecret)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}