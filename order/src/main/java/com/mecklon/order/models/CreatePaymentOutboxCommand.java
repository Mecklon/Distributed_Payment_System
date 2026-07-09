package com.mecklon.order.models;


import com.mecklon.order.models.types.CreatePaymentOutboxCommandStatus;
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
public class CreatePaymentOutboxCommand {

    @Id
    private String id;
    private String orderId;
    private Double price;
    private CreatePaymentOutboxCommandStatus status;

    private UUID leasedBy;
    private Instant createdAt;
    private Instant leasedUntil;

}
