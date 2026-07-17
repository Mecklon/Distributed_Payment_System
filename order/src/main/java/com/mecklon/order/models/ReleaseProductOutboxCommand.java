package com.mecklon.order.models;


import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.core.dtos.ProductReservationInfoDTO;
import com.mecklon.order.models.types.ReleaseProductOutboxCommandStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseProductOutboxCommand {
    @Id
    private String id;
    private String orderId;
    private List<ProductReservationInfoDTO> productList;
    private ReleaseProductOutboxCommandStatus status;
    private UUID leasedBy;
    private Instant leasedUntil;
    private Instant createdAt;
    private Boolean paymentExpired;
}
