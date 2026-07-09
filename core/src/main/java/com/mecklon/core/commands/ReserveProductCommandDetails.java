package com.mecklon.core.commands;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReserveProductCommandDetails {
    private String productId;
    private Integer quantity;
}
