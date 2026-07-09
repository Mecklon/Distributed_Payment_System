package com.mecklon.order.models;


import com.mecklon.order.models.types.PropagateReserveOrderOutBoxCommandStatus;
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
public class PropagateReserveOrderOutboxCommand {
    @Id
    private String id;
    private String orderId;
    private PropagateReserveOrderOutBoxCommandStatus status;
    private UUID leasedBy;
    private Instant leasedUntil;
    private Instant createdAt;
}
