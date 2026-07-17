package com.mecklon.core.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentExpiredEvent {
    private String orderId;
    private String paymentId;
}
