package com.mecklon.payment.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mecklon.payment.services.PaymentService;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String body,
            @RequestHeader("X-Razorpay-Signature") String signature) throws RazorpayException, RazorpayException, JsonProcessingException {

        System.out.println("got web hook");
        boolean valid = Utils.verifyWebhookSignature(
                body,
                signature,
                webhookSecret
        );

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        paymentService.handleRazorpayWebhookPayload(body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public void test(){
        System.out.println("hit");
    }
}
