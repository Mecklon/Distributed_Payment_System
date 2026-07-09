package com.mecklon.product.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddProductRequest {
    private String name;
    private Double rating;
    private String description;
    private String category;
    private Double price;
    private Long stock;
}
