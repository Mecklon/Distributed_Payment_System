package com.mecklon.payment;


import com.mecklon.payment.models.CreatedPaymentOutboxEvent;
import com.mecklon.payment.models.FailedCreatedPaymentOutboxEvent;
import com.mecklon.payment.models.GetOrderIdOutboxRequest;
import com.mecklon.payment.models.Payment;
import com.mecklon.payment.models.types.FailedCreatedPaymentOutboxEventStatus;
import com.mecklon.payment.repositories.CreatedPaymentOutboxEventRepository;
import com.mecklon.payment.repositories.FailedCreatedPaymentOutboxEventRepository;
import com.mecklon.payment.repositories.GetOrderIdOutBoxRequestRepository;
import com.mecklon.payment.repositories.PaymentRepository;
import com.mecklon.payment.models.types.CreatedPaymentOutboxEventStatus;
import com.mecklon.payment.models.types.GetOrderIdOutboxRequestStatus;
import com.mecklon.payment.models.types.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransactions {


    private final PaymentRepository paymentRepository;
    private final GetOrderIdOutBoxRequestRepository getOrderIdOutBoxRequestRepository;
    private final CreatedPaymentOutboxEventRepository createdPaymentOutboxEventRepository;
    private final FailedCreatedPaymentOutboxEventRepository failedCreatedPaymentOutboxEventRepository;


    @Transactional
    public void savePaymentWithOutboxEvent(Payment payment, GetOrderIdOutboxRequest request){
        Payment savedPayment = paymentRepository.save(payment);
        request.setPaymentId(savedPayment.getPaymentId());
        getOrderIdOutBoxRequestRepository.save(request);
    }

    @Transactional
    public void saveOrderIdCreationSuccessful(GetOrderIdOutboxRequest claimed){
        claimed.setStatus(GetOrderIdOutboxRequestStatus.PROPAGATED);
        getOrderIdOutBoxRequestRepository.save(claimed);
        Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
        payment.setUpdateAt(Instant.now());
        payment.setStatus(PaymentStatus.ORDER_CREATED);
        payment.setRazorpayOrderId(UUID.randomUUID().toString());
        paymentRepository.save(payment);
        CreatedPaymentOutboxEvent event = CreatedPaymentOutboxEvent.builder()
                .paymentId(claimed.getPaymentId())
                .orderId(claimed.getOrderId())
                .status(CreatedPaymentOutboxEventStatus.CREATED)
                .createdAt(Instant.now())
                .razorPayOrderId(payment.getRazorpayOrderId())
                .build();
        createdPaymentOutboxEventRepository.save(event);
    }

    @Transactional
    public void saveOrderIdCreationFailed(GetOrderIdOutboxRequest claimed){
        claimed.setStatus(GetOrderIdOutboxRequestStatus.PROPAGATED);
        getOrderIdOutBoxRequestRepository.save(claimed);
        Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
        payment.setUpdateAt(Instant.now());
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        FailedCreatedPaymentOutboxEvent event = FailedCreatedPaymentOutboxEvent.builder()
                .paymentId(claimed.getPaymentId())
                .orderId(claimed.getOrderId())
                .createdAt(Instant.now())
                .status(FailedCreatedPaymentOutboxEventStatus.CREATED)
                .build();
        failedCreatedPaymentOutboxEventRepository.save(event);
    }
}
