package com.mecklon.payment.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.mecklon.payment.PaymentTransactions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ObjectMapper objectMapper;
    private final PaymentTransactions paymentTransactions;

    public void handleRazorpayWebhookPayload(String body) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);

        String event = root.get("event").asText();

        JsonNode payment = root
                .path("payload")
                .path("payment")
                .path("entity");

        String razorpayPaymentId = payment.path("id").asText();
        String status = payment.path("status").asText();
        String razorpayOrderId = payment.path("order_id").asText();
        String internalPaymentId = payment
                .path("notes")
                .path("internalPaymentId")
                .asText();


        System.out.println("razorpay payment id: "+razorpayPaymentId);
        System.out.println("razorpay status: "+status);
        System.out.println("razorpay order id: "+razorpayOrderId);
        System.out.println("internal payment id: " +internalPaymentId);



        if(status.equals("captured")){
            paymentTransactions.markPaymentSucceessfullAndUpdateOutbox( razorpayOrderId, internalPaymentId);
        }
    }
}
