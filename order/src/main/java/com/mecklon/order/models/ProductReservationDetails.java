package com.mecklon.order.models;


import com.mecklon.order.models.types.ProductReservationDetailsStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReservationDetails {

    private String productId;
    private Integer quantity;
    private ProductReservationDetailsStatus status;
}
