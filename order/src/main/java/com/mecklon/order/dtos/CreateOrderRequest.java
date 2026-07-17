package com.mecklon.order.dtos;


import com.mecklon.core.dtos.ProductDetailsDTO;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {
    private String checkoutSessionId;
    private List<ProductDetailsDTO> products;
}
