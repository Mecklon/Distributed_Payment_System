package com.mecklon.payment.models;


import com.mecklon.payment.models.types.FailedCreatedPaymentOutboxEventStatus;
import com.mecklon.payment.models.types.PaymentSuccessfulEventOutboxEventStatus;
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
public class PaymentSuccessfulEventOutboxEvent {
    @Id
    private String id;
    private String orderId;
    private String paymentId;


    private UUID leasedBy;
    private Instant createdAt;
    private Instant leasedUntil;
    private PaymentSuccessfulEventOutboxEventStatus status;
}
