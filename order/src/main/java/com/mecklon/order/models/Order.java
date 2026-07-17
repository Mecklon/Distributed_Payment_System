package com.mecklon.order.models;


import com.mecklon.order.models.types.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String orderId;
    @Indexed
    private String userId;
    private String paymentId;
    private String checkoutSessionId;
    private List<ProductReservationDetails> products;
    private OrderStatus status;
    private Double totalPrice;
    private Instant createdAt;
    @Builder.Default
    private List<SagaEventHistory> history = new ArrayList<>();
}
