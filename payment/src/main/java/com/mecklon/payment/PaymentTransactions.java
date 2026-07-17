package com.mecklon.payment;


import com.mecklon.payment.models.*;
import com.mecklon.payment.models.types.*;
import com.mecklon.payment.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransactions {


    private final PaymentRepository paymentRepository;
    private final GetOrderIdOutBoxRequestRepository getOrderIdOutBoxRequestRepository;
    private final CreatedPaymentOutboxEventRepository createdPaymentOutboxEventRepository;
    private final FailedCreatedPaymentOutboxEventRepository failedCreatedPaymentOutboxEventRepository;
    private final CheckPaymentStatusOutboxEventRepository checkPaymentStatusOutboxEventRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentSuccessfulEventOutboxEventRepository paymentSuccessfulEventOutboxEventRepository;

    @Transactional
    public void savePaymentWithOutboxEvent(Payment payment, GetOrderIdOutboxRequest request){
        Payment savedPayment = paymentRepository.save(payment);
        request.setPaymentId(savedPayment.getPaymentId());
        getOrderIdOutBoxRequestRepository.save(request);
    }

    @Transactional
    public void saveOrderIdCreationSuccessful(GetOrderIdOutboxRequest claimed, String razorpayOrderId){
        claimed.setStatus(GetOrderIdOutboxRequestStatus.PROPAGATED);
        getOrderIdOutBoxRequestRepository.save(claimed);
        Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
        payment.setUpdateAt(Instant.now());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId(razorpayOrderId);
        paymentRepository.save(payment);
        CreatedPaymentOutboxEvent event = CreatedPaymentOutboxEvent.builder()
                .paymentId(claimed.getPaymentId())
                .orderId(claimed.getOrderId())
                .status(CreatedPaymentOutboxEventStatus.CREATED)
                .createdAt(Instant.now())
                .razorPayOrderId(payment.getRazorpayOrderId())
                .build();
        createdPaymentOutboxEventRepository.save(event);
        CheckPaymentStatusOutboxEvent checkPaymentStatusOutboxEvent = CheckPaymentStatusOutboxEvent.builder()
                .paymentId(payment.getPaymentId())
                .razorpayOrderId(razorpayOrderId)
                .status(CheckPaymentStatusOutboxEventStatus.PENDING)
                .createdAt(Instant.now())
                .leasedUntil(Instant.now())
                .nextCheckTime(Instant.now().plus(Duration.ofSeconds(10)))
                .build();
        checkPaymentStatusOutboxEventRepository.save(checkPaymentStatusOutboxEvent);
    }

    @Transactional
    public void saveOrderIdCreationFailed(GetOrderIdOutboxRequest claimed){
        claimed.setStatus(GetOrderIdOutboxRequestStatus.PROPAGATED);
        getOrderIdOutBoxRequestRepository.save(claimed);
        Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
        payment.setUpdateAt(Instant.now());
        payment.setStatus(PaymentStatus.INITIALIZATION_FAILED);
        paymentRepository.save(payment);
        FailedCreatedPaymentOutboxEvent event = FailedCreatedPaymentOutboxEvent.builder()
                .paymentId(claimed.getPaymentId())
                .orderId(claimed.getOrderId())
                .createdAt(Instant.now())
                .status(FailedCreatedPaymentOutboxEventStatus.CREATED)
                .leasedUntil(Instant.now())
                .build();
        failedCreatedPaymentOutboxEventRepository.save(event);
    }

    @Transactional
    public void markPaymentSucceessfullAndUpdateOutbox( String razorpayOrderId, String internalPaymentId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("paymentId").is(internalPaymentId),
                Criteria.where("status").is(PaymentStatus.PENDING)  // expiry check
        ));
        Update update = new Update().set("status", PaymentStatus.PAID);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        Payment savedPayment =  mongoTemplate.findAndModify(query, update, options, Payment.class);
        System.out.println("marking payment successfull");
        if(savedPayment==null)return;
        PaymentSuccessfulEventOutboxEvent paymentSuccessfulEventOutboxEvent =
                PaymentSuccessfulEventOutboxEvent.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .orderId(savedPayment.getOrderId())
                        .status(PaymentSuccessfulEventOutboxEventStatus.CREATED)
                        .build();
        System.out.println("saving payment successfull outbox event");
        paymentSuccessfulEventOutboxEventRepository.save(paymentSuccessfulEventOutboxEvent);
    }
    private RefundLatePaymentOutboxRepository refundLatePaymentOutboxRepository;


    @Transactional
    public void markPaymentSucceessfullAndUpdateOutboxByCleanUp( String razorpayOrderId, String internalPaymentId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("paymentId").is(internalPaymentId),
                Criteria.where("status").is(PaymentStatus.PENDING)  // expiry check
        ));
        Update update = new Update().set("status", PaymentStatus.PAID);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        Payment savedPayment =  mongoTemplate.findAndModify(query, update, options, Payment.class);
        if(savedPayment==null){ // meaning the clean up lost to webhook success or payment was already expired
            Payment currentPayment = paymentRepository.findByPaymentId(internalPaymentId);
            // we check again if the payment was set to paid or it was actually expired
            if(currentPayment.getStatus()==PaymentStatus.EXPIRED){
                //since expired we set have to refund so create a outbox for it and save the current payment as to be refunded
                currentPayment.setStatus(PaymentStatus.TO_BE_REFUNDED);
                paymentRepository.save(currentPayment);
                RefundLatePaymentOutbox refundLatePaymentOutbox = RefundLatePaymentOutbox.builder()
                        .razorpayOrderId(currentPayment.getRazorpayOrderId())
                        .razorPayPaymentId(currentPayment.getRazorpayPaymentId())
                        .paymentId(currentPayment.getPaymentId())
                        .idempotencyKey(UUID.randomUUID().toString())
                        .status(RefundLatePaymentOutboxStatus.CREATED)
                        .build();
                refundLatePaymentOutboxRepository.save(refundLatePaymentOutbox);
            }
        }else{
            PaymentSuccessfulEventOutboxEvent paymentSuccessfulEventOutboxEvent =
                    PaymentSuccessfulEventOutboxEvent.builder()
                            .paymentId(savedPayment.getPaymentId())
                            .orderId(savedPayment.getOrderId())
                            .status(PaymentSuccessfulEventOutboxEventStatus.CREATED)
                            .build();
            paymentSuccessfulEventOutboxEventRepository.save(paymentSuccessfulEventOutboxEvent);
        }

    }

    private final PaymentExpiredOutboxEventRepository paymentExpiredOutboxEventRepository;

    @Transactional
    public void markPaymentExpiredWithSaveOutbox(String internalPaymentId){
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("paymentId").is(internalPaymentId),
                Criteria.where("status").is(PaymentStatus.PENDING)  // success check
        ));
        Update update = new Update().set("status", PaymentStatus.EXPIRED);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        Payment savedPayment =  mongoTemplate.findAndModify(query, update, options, Payment.class);
        if(savedPayment==null)return;

        PaymentExpiredOutboxEvent paymentExpiredOutboxEvent = PaymentExpiredOutboxEvent.builder()
                .paymentId(savedPayment.getPaymentId())
                .orderId(savedPayment.getOrderId())
                .status(PaymentExpiredOutboxEventStatus.CREATED)
                .build();
        paymentExpiredOutboxEventRepository.save(paymentExpiredOutboxEvent);
    }


    private final RefundStatusUpdateOutboxEventRepository refundStatusUpdateOutboxEventRepository;

    @Transactional
    public void savePaymentDataWithRefundOutbox(RefundLatePaymentOutbox claimed, Payment payment, boolean success) {
        refundLatePaymentOutboxRepository.save(claimed);
        paymentRepository.save(payment);
        refundStatusUpdateOutboxEventRepository.save(RefundStatusUpdateOutboxEvent.builder()
                        .refundSuccessful(success)
                        .orderId(payment.getOrderId())
                        .status(RefundStatusUpdateOutboxEventStatus.CREATED)
                .build());
    }
}
