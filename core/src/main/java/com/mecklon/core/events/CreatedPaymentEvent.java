package com.mecklon.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatedPaymentEvent {
    private String orderId;
    private String paymentId;
    private String razorPayOrderId;
}
