package com.mecklon.core.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailsDTO {
    private String productId;
    private String name;
    private String category;
    private Double price;
    private String imgName;
    private Integer quantity;
}
