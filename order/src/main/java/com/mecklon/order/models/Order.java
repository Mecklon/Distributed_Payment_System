package com.mecklon.order.models;


import com.mecklon.order.models.types.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String userId;
    private String paymentId;
    private List<ProductReservationDetails> products;
    private OrderStatus status;
    private Double totalPrice;
}
