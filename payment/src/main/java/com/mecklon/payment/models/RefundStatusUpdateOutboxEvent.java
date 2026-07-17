package com.mecklon.payment.models;


import com.mecklon.payment.models.types.RefundStatusUpdateOutboxEventStatus;
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
public class RefundStatusUpdateOutboxEvent {
    @Id
    private String id;
    private String orderId;
    private Boolean refundSuccessful;
    private UUID leasedBy;
    private Instant leasedUntil;
    private RefundStatusUpdateOutboxEventStatus status;
}
