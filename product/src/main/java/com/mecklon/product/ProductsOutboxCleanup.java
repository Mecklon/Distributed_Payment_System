package com.mecklon.product;

import com.mecklon.core.commands.CreatePaymentCommand;
import com.mecklon.core.events.FailedReservedProductEvent;
import com.mecklon.core.events.ReleasedProductEvent;
import com.mecklon.core.events.ReservedProductEvent;
import com.mecklon.product.model.FailedReservedProductOutboxEvent;
import com.mecklon.product.model.ReleasedProductOutboxEvent;
import com.mecklon.product.model.ReservedProductOutboxEvent;
import com.mecklon.product.model.types.FailedReservedProductOutboxEventStatus;
import com.mecklon.product.model.types.ReleasedProductOutboxEventStatus;
import com.mecklon.product.model.types.ReservedProductOutboxEventStatus;
import com.mecklon.product.repositories.FailedReservedProductOutboxEventRepository;
import com.mecklon.product.repositories.ReleasedProductOutboxEventRepository;
import com.mecklon.product.repositories.ReservedProductOutBoxEventRepository;
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
public class ProductsOutboxCleanup {

    private final KafkaTemplate<String, ReservedProductEvent> kafkaTemplate;
    private final MongoTemplate mongoTemplate;
    private final ReservedProductOutBoxEventRepository reservedProductOutBoxEventRepository;

    @Value("${reserved-product-event}")
    private String reservedProductEventTopic;

    @Scheduled(fixedDelay = 5000)
    public void propagateProductReservedEvent(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();
        System.out.println("running reserved produt event propogation");

        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(ReservedProductOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(ReservedProductOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<ReservedProductOutboxEvent> outboxCommands = mongoTemplate.find(query, ReservedProductOutboxEvent.class);
        System.out.println(outboxCommands.size());

        for(ReservedProductOutboxEvent outboxCommand: outboxCommands){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(ReservedProductOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(ReservedProductOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",ReservedProductOutboxEventStatus.PROCESSING);

            ReservedProductOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            ReservedProductOutboxEvent.class
                    );

            if(claimed==null)continue;

            ReservedProductEvent event = new ReservedProductEvent(claimed.getOrderId(), claimed.getTotalPrice());
            System.out.println("sending reserved product");
            kafkaTemplate.send(reservedProductEventTopic,claimed.getOrderId(), event);
            System.out.println("sent reserved product");

            claimed.setStatus(ReservedProductOutboxEventStatus.PROPAGATED);
            reservedProductOutBoxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpFinishedPropagations(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(ReservedProductOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, ReservedProductOutboxEvent.class);
    }


    @Value("${failed-reserved-product-event}")
    private String failedReservedProductEventTopic;

    private final FailedReservedProductOutboxEventRepository failedReservedProductOutboxEventRepository;
    private final KafkaTemplate<String, FailedReservedProductEvent> failedReservedProductEventKafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void propagateFailedReservedProductEvent(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(FailedReservedProductOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(FailedReservedProductOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<FailedReservedProductOutboxEvent> outboxEvents = mongoTemplate.find(query, FailedReservedProductOutboxEvent.class);


        for(FailedReservedProductOutboxEvent outboxEvent: outboxEvents){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxEvent.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(FailedReservedProductOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(FailedReservedProductOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );
            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",ReservedProductOutboxEventStatus.PROCESSING);

            FailedReservedProductOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            FailedReservedProductOutboxEvent.class
                    );

            if(claimed==null)continue;

            FailedReservedProductEvent event = new FailedReservedProductEvent(claimed.getOrderId());
            failedReservedProductEventKafkaTemplate.send(failedReservedProductEventTopic,claimed.getOrderId(), event);
            claimed.setStatus(FailedReservedProductOutboxEventStatus.PROPAGATED);
            failedReservedProductOutboxEventRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanupPropagatedFailedReservedProductsEvents(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(FailedReservedProductOutboxEventStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, FailedReservedProductOutboxEvent.class);
    }


    @Value("${released-product-event}")
    private String releasedProductEventTopic;

    private final KafkaTemplate<String, ReleasedProductEvent> releasedProductEventKafkaTemplate;
    private final ReleasedProductOutboxEventRepository releasedProductOutboxEventRepository;

    @Scheduled(fixedDelay = 5000)
    public void propagateReleasedProductEvent(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(ReleasedProductOutboxEventStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(ReleasedProductOutboxEventStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<ReleasedProductOutboxEvent> outboxCommands = mongoTemplate.find(query, ReleasedProductOutboxEvent.class);

        for(ReleasedProductOutboxEvent outboxCommand: outboxCommands){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(ReleasedProductOutboxEventStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(ReleasedProductOutboxEventStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",ReleasedProductOutboxEventStatus.PROCESSING);

            ReleasedProductOutboxEvent claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            ReleasedProductOutboxEvent.class
                    );

            if(claimed==null)continue;

            ReleasedProductEvent event = new ReleasedProductEvent(claimed.getOrderId());
            releasedProductEventKafkaTemplate.send(releasedProductEventTopic,claimed.getOrderId(),event);
            claimed.setStatus(ReleasedProductOutboxEventStatus.PROPAGATED);
            releasedProductOutboxEventRepository.save(claimed);
        }
    }
}
