package com.mecklon.order.dtos;


import com.mecklon.order.models.ProductReservationDetails;
import com.mecklon.order.models.SagaEventHistory;
import com.mecklon.order.models.types.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private String orderId;
    private OrderStatus status;
    private Double totalPrice;
    private List<ProductReservationDetails> products;
    private Instant createdAt;
    private List<SagaEventHistory> history = new ArrayList<>();
}
