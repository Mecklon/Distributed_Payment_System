package com.mecklon.payment;


import com.mecklon.core.commands.CreatePaymentCommand;
import com.mecklon.payment.models.GetOrderIdOutboxRequest;
import com.mecklon.payment.models.Payment;
import com.mecklon.payment.repositories.PaymentRepository;
import com.mecklon.payment.models.types.GetOrderIdOutboxRequestStatus;
import com.mecklon.payment.models.types.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentsKafkaConsumer {

    private final PaymentTransactions paymentTransactions;
    private final PaymentRepository paymentRepository;
    // remember to make this transactional,
    @KafkaListener(topics = "${create-payment-command}")
    public void getPaymentId(@Payload CreatePaymentCommand command){
        if(paymentRepository.existsByOrderId(command.getOrderId())){
            return;
        }

        Payment payment = Payment.builder()
                .orderId(command.getOrderId())
                .status(PaymentStatus.CREATED)
                .amount(command.getPrice())
                .idempotencyKey(UUID.randomUUID().toString())
                .currency("INR")
                .createdAt(Instant.now())
                .build();
        GetOrderIdOutboxRequest request = GetOrderIdOutboxRequest.builder()
                .orderId(command.getOrderId())
                .createdAt(Instant.now())
                .status(GetOrderIdOutboxRequestStatus.CREATED)
                .build();


        paymentTransactions.savePaymentWithOutboxEvent(payment, request);
    }
}
