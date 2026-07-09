package com.mecklon.payment.models;


import com.mecklon.payment.models.types.CreatedPaymentOutboxEventStatus;
import com.mecklon.payment.models.types.FailedCreatedPaymentOutboxEventStatus;
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
public class FailedCreatedPaymentOutboxEvent {

    @Id
    private String id;
    private String paymentId;
    private String orderId;

    private UUID leasedBy;
    private Instant createdAt;
    private Instant leasedUntil;
    private FailedCreatedPaymentOutboxEventStatus status;
}
