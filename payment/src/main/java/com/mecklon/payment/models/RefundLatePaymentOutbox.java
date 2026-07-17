package com.mecklon.payment.models;


import com.mecklon.payment.models.types.PaymentSuccessfulEventOutboxEventStatus;
import com.mecklon.payment.models.types.RefundLatePaymentOutboxStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundLatePaymentOutbox {
    @Id
    private String id;

    private String razorpayOrderId;
    private String razorPayPaymentId;
    private String paymentId;

    private String idempotencyKey;
    private int retriesAvailable;
    private UUID leasedBy;
    private Instant leasedUntil;
    private RefundLatePaymentOutboxStatus status;
}
