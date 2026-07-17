package com.mecklon.order;

import com.mecklon.core.commands.CreatePaymentCommand;
import com.mecklon.core.commands.ReleaseProductCommand;
import com.mecklon.core.commands.ReserveProductCommand;
import com.mecklon.core.commands.ReserveProductCommandDetails;
import com.mecklon.order.models.*;
import com.mecklon.order.models.types.CreatePaymentOutboxCommandStatus;
import com.mecklon.order.models.types.PropagateReserveOrderOutBoxCommandStatus;
import com.mecklon.order.models.types.ReleaseProductOutboxCommandStatus;
import com.mecklon.order.models.types.SagaEventHistoryStatus;
import com.mecklon.order.repositories.CreatePaymentOutBoxCommandRepository;
import com.mecklon.order.repositories.OrderRepository;
import com.mecklon.order.repositories.PropagateReserveOrderOutboxCommandRepository;
import com.mecklon.order.repositories.ReleaseProductOutboxCommandRepository;
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
public class OutboxEventScheduler {

    private final MongoTemplate mongoTemplate;
    private final OrderRepository orderRepository;
    private final PropagateReserveOrderOutboxCommandRepository propagateReserveOrderOutboxCommandRepository;
    private final KafkaTemplate<String, ReserveProductCommand> kafkaTemplate;
    private final CreatePaymentOutBoxCommandRepository createPaymentOutBoxCommandRepository;
    private final KafkaTemplate<String, CreatePaymentCommand> CreatePaymentCommandKafkaTemplate;

    @Value("${reserve-product-command}")
    private String reserveProductCommandTopic;

    @Value("${create-payment-command}")
    private String createPaymentCommandTopic;


    @Scheduled(fixedDelay = 5000)
    public void propagateReserveOrderOutboxCommand(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(PropagateReserveOrderOutBoxCommandStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(PropagateReserveOrderOutBoxCommandStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<PropagateReserveOrderOutboxCommand> outboxCommands = mongoTemplate.find(query, PropagateReserveOrderOutboxCommand.class);

        for(PropagateReserveOrderOutboxCommand outboxCommand: outboxCommands){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(PropagateReserveOrderOutBoxCommandStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(PropagateReserveOrderOutBoxCommandStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",PropagateReserveOrderOutBoxCommandStatus.PROCESSING);

            PropagateReserveOrderOutboxCommand claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            PropagateReserveOrderOutboxCommand.class
                    );

            if(claimed == null)continue;

            Update orderUpdate = new Update();
            orderUpdate.push("history", new SagaEventHistory(Instant.now(),false, SagaEventHistoryStatus.RESERVE_PRODUCT_COMMAND));
            Query orderQuery = new Query();
            orderQuery.addCriteria(Criteria.where("orderId").is(claimed.getOrderId()));
            mongoTemplate.findAndModify(
                    orderQuery,
                    orderUpdate,
                    FindAndModifyOptions.options().returnNew(false),
                    Order.class
            );

            Order order = orderRepository.findById(claimed.getOrderId()).orElse(null);
            if(order==null){
                claimed.setStatus(PropagateReserveOrderOutBoxCommandStatus.PROPAGATED);
                propagateReserveOrderOutboxCommandRepository.save(claimed);
                continue;
            }

            ReserveProductCommand command = new ReserveProductCommand(
                        claimed.getOrderId(),
                        order.getProducts().stream().map(product->{
                            return new ReserveProductCommandDetails(
                                    product.getProductId(),
                                    product.getQuantity()
                            );
                        }).toList()
                    );

            System.out.println("sending receive product command");
            kafkaTemplate.send(reserveProductCommandTopic, claimed.getOrderId(),command);
            System.out.println("sent receive product command");

            claimed.setStatus(PropagateReserveOrderOutBoxCommandStatus.PROPAGATED);
            propagateReserveOrderOutboxCommandRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpFinishedPropagations(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(PropagateReserveOrderOutBoxCommandStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, PropagateReserveOrderOutboxCommand.class);
    }

    @Scheduled(fixedDelay = 5000)
    public void propagateCreatePaymentCommand(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(CreatePaymentOutboxCommandStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(CreatePaymentOutboxCommandStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<CreatePaymentOutboxCommand> outboxCommands = mongoTemplate.find(query, CreatePaymentOutboxCommand.class);

        for(CreatePaymentOutboxCommand outboxCommand: outboxCommands){
            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(CreatePaymentOutboxCommandStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(CreatePaymentOutboxCommandStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",CreatePaymentOutboxCommandStatus.PROCESSING);



            CreatePaymentOutboxCommand claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            CreatePaymentOutboxCommand.class
                    );

            if(claimed==null)continue;

            Update orderUpdate = new Update();
            orderUpdate.push("history", new SagaEventHistory(Instant.now(),false, SagaEventHistoryStatus.CREATE_PAYMENT_COMMAND));
            Query orderQuery = new Query();
            orderQuery.addCriteria(Criteria.where("orderId").is(claimed.getOrderId()));
            mongoTemplate.findAndModify(
                    orderQuery,
                    orderUpdate,
                    FindAndModifyOptions.options().returnNew(false),
                    Order.class
            );

            CreatePaymentCommand command = new CreatePaymentCommand(claimed.getOrderId(), claimed.getPrice());
            CreatePaymentCommandKafkaTemplate.send(createPaymentCommandTopic,claimed.getOrderId(), command);
            claimed.setStatus(CreatePaymentOutboxCommandStatus.PROPAGATED);
            createPaymentOutBoxCommandRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedCreatePaymentCommands(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(CreatePaymentOutboxCommandStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, CreatePaymentOutboxCommand.class);
    }


    @Value("${release-product-command}")
    private String releaseProductsTopic;

    private final KafkaTemplate<String, ReleaseProductCommand> releaseProductCommandKafkaTemplate;
    private final ReleaseProductOutboxCommandRepository releaseProductOutboxCommandRepository;

    @Scheduled(fixedDelay = 5000)
    public void propagateReleaseProducts(){
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();


        Query query = new Query();
        query.addCriteria(
                new Criteria().orOperator(
                        Criteria.where("status").is(ReleaseProductOutboxCommandStatus.CREATED),
                        new Criteria().andOperator(
                                Criteria.where("status").is(ReleaseProductOutboxCommandStatus.PROCESSING),
                                Criteria.where("leasedUntil").lt(now)
                        )
                )
        ).limit(100);

        List<ReleaseProductOutboxCommand> outboxCommands = mongoTemplate.find(query, ReleaseProductOutboxCommand.class);

        for(ReleaseProductOutboxCommand outboxCommand: outboxCommands){

            Query claimQuery = new Query();
            claimQuery.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("id").is(outboxCommand.getId()),
                            new Criteria().orOperator(
                                    Criteria.where("status").is(ReleaseProductOutboxCommandStatus.CREATED),
                                    new Criteria().andOperator(
                                            Criteria.where("status").is(ReleaseProductOutboxCommandStatus.PROCESSING),
                                            Criteria.where("leasedUntil").lt(now)
                                    )
                            )
                    )
            );

            Update update = new Update();
            update.set("leasedBy", workerId);
            update.set("leasedUntil", now.plus(Duration.ofMinutes(2)));
            update.set("status",ReleaseProductOutboxCommandStatus.PROCESSING);


            ReleaseProductOutboxCommand claimed =
                    mongoTemplate.findAndModify(
                            claimQuery,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            ReleaseProductOutboxCommand.class
                    );

            if(claimed == null)continue;

            Update orderUpdate = new Update();

            if(outboxCommand.getPaymentExpired()){
                orderUpdate.push("history", new SagaEventHistory(Instant.now(),true, SagaEventHistoryStatus.RELEASE_PRODUCT_PAYMENT_EXPIRED_COMMAND));
            }else{
                orderUpdate.push("history", new SagaEventHistory(Instant.now(),true, SagaEventHistoryStatus.RELEASE_PRODUCT_ORDER_ID_CREATION_FAILED_COMMAND));
            }
            Query orderQuery = new Query();
            orderQuery.addCriteria(Criteria.where("orderId").is(claimed.getOrderId()));
            mongoTemplate.findAndModify(
                    orderQuery,
                    orderUpdate,
                    FindAndModifyOptions.options().returnNew(false),
                    Order.class
            );

            ReleaseProductCommand command = new ReleaseProductCommand(outboxCommand.getOrderId(), outboxCommand.getProductList());

            releaseProductCommandKafkaTemplate.send(releaseProductsTopic, claimed.getOrderId(), command);
            claimed.setStatus(ReleaseProductOutboxCommandStatus.PROPAGATED);
            releaseProductOutboxCommandRepository.save(claimed);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void cleanUpPropagatedReleaseProductsDueToFailedOrderIdCreation(){
        Instant now = Instant.now();
        Instant aDayAgo = now.minus(Duration.ofHours(24));
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("status").is(ReleaseProductOutboxCommandStatus.PROPAGATED),
                Criteria.where("createdAt").lt(aDayAgo)
        ));
        mongoTemplate.remove(query, ReleaseProductOutboxCommand.class);
    }

}
