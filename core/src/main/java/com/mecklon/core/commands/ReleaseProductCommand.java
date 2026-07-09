package com.mecklon.core.commands;


import com.mecklon.core.dtos.ProductDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseProductCommand {
    private String orderId;
    private List<ProductDetailsDTO> productList;
}
