package com.mecklon.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPaymentsResponse {
    private int count;
    private List<RazorpayPayment> payments;
}
