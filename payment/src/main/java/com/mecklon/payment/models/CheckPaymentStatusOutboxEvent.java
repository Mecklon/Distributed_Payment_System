package com.mecklon.payment.models;

import com.mecklon.payment.models.types.CheckPaymentStatusOutboxEventStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPaymentStatusOutboxEvent {

    @Id
    private String id;
    @Indexed
    private String paymentId;
    private String razorpayOrderId;
    private CheckPaymentStatusOutboxEventStatus status;
    private Instant createdAt;
    private Instant nextCheckTime;

    private UUID leasedBy;
    private Instant leasedUntil;

}
