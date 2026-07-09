package com.mecklon.product.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private String name;
    private Double rating;
    private String description;
    private String category;
    private Double price;
    private Long stock;
    private String imgName;
}
