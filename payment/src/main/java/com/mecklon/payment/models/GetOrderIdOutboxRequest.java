package com.mecklon.payment.models;


import com.mecklon.payment.models.types.CreatedPaymentOutboxEventStatus;
import com.mecklon.payment.models.types.GetOrderIdOutboxRequestStatus;
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
public class GetOrderIdOutboxRequest {

    @Id
    private String id;
    private String orderId;
    private String paymentId;

    private UUID leasedBy;
    private Instant leasedUntil;
    private Instant createdAt;
    private GetOrderIdOutboxRequestStatus status;

}
