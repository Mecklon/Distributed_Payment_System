package com.mecklon.product.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    private String id;
    private String name;
    private Double rating;
    private String description;
    private String category;
    private Double price;
    private Long stock;
    private String imgUrl;
    private String imgName;
}