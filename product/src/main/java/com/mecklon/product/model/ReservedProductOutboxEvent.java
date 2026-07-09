package com.mecklon.product.model;


import com.mecklon.product.model.types.ReservedProductOutboxEventStatus;
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
public class ReservedProductOutboxEvent {
    @Id
    private String id;
    private String orderId;
    private Double totalPrice;

    private UUID leasedBy;
    private Instant leasedUntil;
    private Instant createdAt;
    private ReservedProductOutboxEventStatus status;
}
