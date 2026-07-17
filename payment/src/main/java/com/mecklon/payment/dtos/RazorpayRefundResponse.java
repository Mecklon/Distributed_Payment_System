package com.mecklon.payment.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayRefundResponse{
        private String id;
        private String payment_id;
        private String status;
        private int amount;
}