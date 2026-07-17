package com.mecklon.core.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRefundStatusUpdateEvent {
    private String orderId;
    private Boolean refundSuccessful;
}

