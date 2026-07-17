package com.mecklon.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {

    private String id;
    private String entity;
    private Long amount;
    private String currency;
    private String status;
}