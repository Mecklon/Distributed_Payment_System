package com.mecklon.core.commands;


import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.core.dtos.ProductReservationInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseProductCommand {
    private String orderId;
    private List<ProductReservationInfoDTO> productList;
}
