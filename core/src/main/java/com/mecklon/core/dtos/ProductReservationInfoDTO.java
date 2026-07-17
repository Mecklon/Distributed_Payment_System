package com.mecklon.core.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReservationInfoDTO {
    private String productId;
    private Integer quantity;
}
