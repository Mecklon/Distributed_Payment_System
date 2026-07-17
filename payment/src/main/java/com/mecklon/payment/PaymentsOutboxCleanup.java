package com.mecklon.payment;


import com.mecklon.core.commands.CreatePaymentCommand;
import com.mecklon.core.commands.ReleaseProductCommand;
import com.mecklon.core.events.*;
import com.mecklon.payment.dtos.*;
import com.mecklon.payment.models.*;
import com.mecklon.payment.models.types.*;
import com.mecklon.payment.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PaymentsOutboxCleanup {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, CreatedPaymentEvent> kafkaTemplate;


    @Value("${created-payment-event}")
    private String createdPaymentEventTopic;
    private final CreatedPaymentOutboxEventRepository createdPaymentOutboxEventRepository;


    @Scheduled(fixedDelay = 5000)
    public void propagateCreatedPaymentEvent() {
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();

        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(CreatedPaymentOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(CreatedPaymentOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<CreatedPaymentOutboxEvent> outboxEvents = mongoTemplate.find(query, CreatedPaymentOutboxEvent.class);

        for (CreatedPaymentOutboxEvent outboxEvent : outboxEvents) {
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxEvent.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(CreatedPaymentOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(CreatedPaymentOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status", CreatedPaymentOutboxEventStatus.PROCESSING);

            CreatedPaymentOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            CreatedPaymentOutboxEvent.class
                    );

            if (claimed == null) continue;
            System.out.println(claimed.getOrderId());
            CreatedPaymentEvent event = new CreatedPaymentEvent(claimed.getOrderId(), claimed.getPaymentId(), claimed.getRazorPayOrderId());
            kafkaTemplate.send(createdPaymentEventTopic,event.getOrderId(), event);
            claimed.setStatus(CreatedPaymentOutboxEventStatus.PROPAGATED);
            createdPaymentOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedCreatePaymentEvents() {
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(CreatedPaymentOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, CreatedPaymentOutboxEvent.class);
    }


    private final GetOrderIdOutBoxRequestRepository getOrderIdOutBoxRequestRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactions paymentTransactions;
    private final RazorpayService razorpayService;

    @Scheduled(fixedDelay = 5000)
    private void getOrderId() {
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(GetOrderIdOutboxRequestStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(GetOrderIdOutboxRequestStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<GetOrderIdOutboxRequest> outboxCommands = mongoTemplate.find(query, GetOrderIdOutboxRequest.class);

        for (GetOrderIdOutboxRequest outboxCommand : outboxCommands) {

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(GetOrderIdOutboxRequestStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(GetOrderIdOutboxRequestStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status", GetOrderIdOutboxRequestStatus.PROCESSING);

            GetOrderIdOutboxRequest claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            GetOrderIdOutboxRequest.class
                    );

            if (claimed == null) continue;

            Payment payment = paymentRepository.findByOrderId(claimed.getOrderId());


            try {
                 RazorpayOrderResponse response = razorpayService.createOrder(new RazorpayOrderRequest((long) (payment.getAmount()*100), "INR",payment.getOrderId(), Map.of("internalPaymentId", payment.getPaymentId()))).get();
                // since this will create another kafka producer event transactionally store another outbox event and change the payment status
                System.out.println(response.getId());
                paymentTransactions.saveOrderIdCreationSuccessful(claimed, response.getId());
            } catch (Exception e) {
                // store outbox compensation event and also mark the payment as failed
                paymentTransactions.saveOrderIdCreationFailed(claimed);
            }
        }

    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleaUpPropagatedGetOrderIdRequests() {
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(GetOrderIdOutboxRequestStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, GetOrderIdOutboxRequest.class);
    }



    @Value("${failed-created-payment-event}")
    private String failedCreatedPaymentEventTopic;

    private final FailedCreatedPaymentOutboxEventRepository failedCreatedPaymentOutboxEventRepository;
    private final KafkaTemplate<String, FailedCreatedPaymentEvent > failedCreatedPaymentEventKafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void propagateFailedCreatedPaymentEvent(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(FailedCreatedPaymentOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(FailedCreatedPaymentOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<FailedCreatedPaymentOutboxEvent> outboxCommands = mongoTemplate.find(query, FailedCreatedPaymentOutboxEvent.class);

        for(FailedCreatedPaymentOutboxEvent outboxCommand: outboxCommands){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(FailedCreatedPaymentOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(FailedCreatedPaymentOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",FailedCreatedPaymentOutboxEventStatus.PROCESSING);

            FailedCreatedPaymentOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            FailedCreatedPaymentOutboxEvent.class
                    );

            if(claimed == null)continue;
            FailedCreatedPaymentEvent event = new FailedCreatedPaymentEvent(claimed.getOrderId(), claimed.getPaymentId());
            failedCreatedPaymentEventKafkaTemplate.send(failedCreatedPaymentEventTopic,claimed.getOrderId(), event);
            claimed.setStatus(FailedCreatedPaymentOutboxEventStatus.PROPAGATED);
            failedCreatedPaymentOutboxEventRepository.save(claimed);
        }
    }


    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpFailedPropagatedCreatePaymentCommands() {
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(FailedCreatedPaymentOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, FailedCreatedPaymentOutboxEvent.class);
    }

    @Scheduled(fixedDelay = 5000)
    public void checkRazorpayPaymentStatus(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().andOperator(
                        Criteria.where("status").is(CheckPaymentStatusOutboxEventStatus.PENDING),
                        Criteria.where("leasedUntil").lt(now),
                        Criteria.where("nextCheckTime").lt(now)
                )
        ).limit(100);

        List<CheckPaymentStatusOutboxEvent> outboxEvents = mongoTemplate.find(query, CheckPaymentStatusOutboxEvent.class);

        for(CheckPaymentStatusOutboxEvent outboxEvent: outboxEvents){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxEvent.getId()),
                            Criteria.where("status").is(CheckPaymentStatusOutboxEventStatus.PENDING),
                            Criteria.where("leasedUntil").lt(now)
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofSeconds(10)));

            CheckPaymentStatusOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            CheckPaymentStatusOutboxEvent.class
                    );

            if(claimed == null)continue;
            Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
            if(Duration.between(claimed.getCreatedAt(),Instant.now()).compareTo(Duration.ofHours(24))>0){
                claimed.setStatus(CheckPaymentStatusOutboxEventStatus.COMPLETED);
                checkPaymentStatusOutboxEventRepository.save(claimed);
                continue;
            }
            if(Duration.between( claimed.getCreatedAt(),Instant.now()).compareTo(Duration.ofMinutes(5))<0){
                claimed.setNextCheckTime(Instant.now().plus(Duration.ofMinutes(1)));
            }else{
                claimed.setNextCheckTime(Instant.now().plus(Duration.ofHours(1)));
            }
            if(Duration.between(claimed.getCreatedAt(),Instant.now()).compareTo(Duration.ofMinutes(5))>0 && payment.getStatus()!=PaymentStatus.EXPIRED){
                paymentTransactions.markPaymentExpiredWithSaveOutbox(payment.getPaymentId());
                continue;
            }

            try {
                RazorpayPaymentsResponse response = razorpayService.getPayments(claimed.getRazorpayOrderId()).get();
                Optional<RazorpayPayment> capturedPayment =
                        response.getPayments()
                                .stream()
                                .filter(p -> "captured".equals(p.getStatus()))
                                .findFirst();
                if(capturedPayment.isPresent()){
                    paymentTransactions.markPaymentSucceessfullAndUpdateOutboxByCleanUp(claimed.getRazorpayOrderId(), claimed.getPaymentId());
                }
            }catch(Exception e){
                continue;
            }

            checkPaymentStatusOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpFinishedPropagations(){
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(Duration.ofHours(48));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(CheckPaymentStatusOutboxEventStatus.COMPLETED),
                Criteria.where("createdAt").lt(twoDaysAgo)
        ));
        mongoTemplate.remove(query, CheckPaymentStatusOutboxEvent.class);
    }
    // add clean up to delete expired check payment status outbox


    @Value("${successful-payment-event}")
    private String successfulPaymentEventTopic;

    private final KafkaTemplate<String, PaymentSuccessfulEvent> paymentSuccessfulEventKafkaTemplate;
    private final PaymentSuccessfulEventOutboxEventRepository paymentSuccessfulEventOutboxEventRepository;


    @Scheduled(fixedDelay = 5000)
    public void propagateCreatePaymentCommand(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(PaymentSuccessfulEventOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(PaymentSuccessfulEventOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<PaymentSuccessfulEventOutboxEvent> outboxCommands = mongoTemplate.find(query, PaymentSuccessfulEventOutboxEvent.class);

        for(PaymentSuccessfulEventOutboxEvent outboxCommand: outboxCommands){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(PaymentSuccessfulEventOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(PaymentSuccessfulEventOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",PaymentSuccessfulEventOutboxEventStatus.PROCESSING);

            PaymentSuccessfulEventOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            PaymentSuccessfulEventOutboxEvent.class
                    );

            if(claimed==null)continue;
            System.out.println("propogating successfull payment");
            PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(claimed.getOrderId(), claimed.getPaymentId());
            paymentSuccessfulEventKafkaTemplate.send(successfulPaymentEventTopic,claimed.getOrderId(), event);
            claimed.setStatus(PaymentSuccessfulEventOutboxEventStatus.PROPAGATED);
            paymentSuccessfulEventOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedPaymentSuccessfulEvent() {
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(PaymentSuccessfulEventOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, PaymentSuccessfulEventOutboxEvent.class);
    }

    @Value("${expired-payment-event}")
    private String expiredPaymentEventTopic;

    private final KafkaTemplate<String, PaymentExpiredEvent> paymentExpiredEventKafkaTemplate;
    private final PaymentExpiredOutboxEventRepository paymentExpiredOutboxEventRepository;
    private final CheckPaymentStatusOutboxEventRepository checkPaymentStatusOutboxEventRepository;
    private final RefundLatePaymentOutboxRepository refundLatePaymentOutboxRepository;

    @Scheduled(fixedDelay = 5000)
    public void propagateExpiredPaymentEvent(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(PaymentExpiredOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(PaymentExpiredOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<PaymentExpiredOutboxEvent> outboxEvents = mongoTemplate.find(query, PaymentExpiredOutboxEvent.class);

        for(PaymentExpiredOutboxEvent outboxCommand: outboxEvents){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(PaymentExpiredOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(PaymentExpiredOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",PaymentExpiredOutboxEventStatus.PROCESSING);

            PaymentExpiredOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            PaymentExpiredOutboxEvent.class
                    );

            if(claimed==null)continue;

            PaymentExpiredEvent event = new PaymentExpiredEvent(claimed.getPaymentId(), claimed.getOrderId());
            paymentExpiredEventKafkaTemplate.send(expiredPaymentEventTopic, claimed.getOrderId(),event);
            claimed.setStatus(PaymentExpiredOutboxEventStatus.PROPAGATED);
            paymentExpiredOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedCreatePaymentCommands(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(PaymentExpiredOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, PaymentExpiredOutboxEvent.class);
    }


    @Scheduled(fixedDelay = 5000)
    public void refundLatePayments(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(RefundLatePaymentOutboxStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(RefundLatePaymentOutboxStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<RefundLatePaymentOutbox> outboxEvents = mongoTemplate.find(query, RefundLatePaymentOutbox.class);

        for(RefundLatePaymentOutbox outboxEvent: outboxEvents){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxEvent.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(RefundLatePaymentOutboxStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(RefundLatePaymentOutboxStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",RefundLatePaymentOutboxStatus.PROCESSING);

            RefundLatePaymentOutbox claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            RefundLatePaymentOutbox.class
                    );

            if(claimed==null)continue;
            Payment payment = paymentRepository.findByPaymentId(claimed.getPaymentId());
            try {
                RazorpayRefundResponse response = razorpayService.refund((int)(payment.getAmount()*100),claimed.getIdempotencyKey() ,claimed.getRazorPayPaymentId()).get();
                if(response.getStatus().equals("processed")){
                    claimed.setStatus(RefundLatePaymentOutboxStatus.PROPAGATED);
                    payment.setStatus(PaymentStatus.REFUNDED);

                    paymentTransactions.savePaymentDataWithRefundOutbox(claimed, payment, true);
                }else if(response.getStatus().equals("failed")){
                    if(claimed.getRetriesAvailable()==0){
                        claimed.setStatus(RefundLatePaymentOutboxStatus.PROPAGATED);
                        payment.setStatus(PaymentStatus.REFUND_FAILED);
                        paymentTransactions.savePaymentDataWithRefundOutbox(claimed, payment, false);
                    }else{
                        claimed.setRetriesAvailable(claimed.getRetriesAvailable()-1);
                        claimed.setIdempotencyKey(UUID.randomUUID().toString());
                        refundLatePaymentOutboxRepository.save(claimed);
                    }
                }
            } catch(Exception e){
                continue;
            }
        }
    }

    @Value("${payment-refund-status-update}")
    private String paymentRefundStatusUpdateTopic;

    private final KafkaTemplate<String,PaymentRefundStatusUpdateEvent> paymentRefundStatusUpdateEventKafkaTemplate;
    private final RefundStatusUpdateOutboxEventRepository refundStatusUpdateOutboxEventRepository;

    @Scheduled(fixedDelay = 5000)
    public void propagateReleaseProductsDueToFailedOrderIdCreation(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(RefundStatusUpdateOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(RefundStatusUpdateOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<RefundStatusUpdateOutboxEvent> outboxEvents = mongoTemplate.find(query, RefundStatusUpdateOutboxEvent.class);

        for(RefundStatusUpdateOutboxEvent outboxEvent: outboxEvents){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxEvent.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(RefundStatusUpdateOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(RefundStatusUpdateOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",RefundStatusUpdateOutboxEventStatus.PROCESSING);

            RefundStatusUpdateOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            RefundStatusUpdateOutboxEvent.class
                    );

            if(claimed == null)continue;

            PaymentRefundStatusUpdateEvent event = new PaymentRefundStatusUpdateEvent(claimed.getOrderId(), claimed.getRefundSuccessful());
            paymentRefundStatusUpdateEventKafkaTemplate.send(paymentRefundStatusUpdateTopic, claimed.getOrderId(),event);
            claimed.setStatus(RefundStatusUpdateOutboxEventStatus.PROPAGATED);
            refundStatusUpdateOutboxEventRepository.save(claimed);
        }
    }

}
