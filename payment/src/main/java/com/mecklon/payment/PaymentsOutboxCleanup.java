package com.mecklon.payment;


import com.mecklon.core.events.CreatedPaymentEvent;
import com.mecklon.core.events.FailedCreatedPaymentEvent;
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
import java.util.UUID;

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
            CreatedPaymentEvent event = new CreatedPaymentEvent(claimed.getOrderId(), claimed.getPaymentId(), claimed.getRazorPayOrderId());
            kafkaTemplate.send(createdPaymentEventTopic, event);
            claimed.setStatus(CreatedPaymentOutboxEventStatus.PROPAGATED);
            createdPaymentOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedCreatePaymentCommands() {
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

                // make the call to the razorpay api
            } catch (Exception e) {
                // store outbox compensation event and also mark the payment as failed
                paymentTransactions.saveOrderIdCreationFailed(claimed);
                continue;
            }
            // since this will create another kafka producer event transactionally store another outbox event and change the payment status
            paymentTransactions.saveOrderIdCreationSuccessful(claimed);
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
            failedCreatedPaymentEventKafkaTemplate.send(failedCreatedPaymentEventTopic, event);
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


}
