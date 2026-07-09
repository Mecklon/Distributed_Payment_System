package com.mecklon.payment.models;


import com.mecklon.payment.models.types.PaymentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    private String paymentId;
    private String orderId;
    private String idempotencyKey;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private PaymentStatus status;
    private Double amount;
    private String currency;
    private Instant createdAt;
    private Instant updateAt;
}
