package com.mecklon.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPayment {
    private String id;
    private int amount;
    private String order_id;
    private String status;
}
