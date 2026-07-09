package com.mecklon.payment.models;

import com.mecklon.payment.models.types.CreatedPaymentOutboxEventStatus;
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
public class CreatedPaymentOutboxEvent {
    @Id
    private String id;
    private String paymentId;
    private String orderId;
    private String razorPayOrderId;

    private UUID leasedBy;
    private Instant leasedUntil;
    private Instant createdAt;
    private CreatedPaymentOutboxEventStatus status;
}
