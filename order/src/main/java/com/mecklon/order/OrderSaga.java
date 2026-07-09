package com.mecklon.order;


import com.mecklon.core.events.*;
import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.order.dtos.WebSocketMessage;
import com.mecklon.order.dtos.types.WebSocketEventType;
import com.mecklon.order.models.*;
import com.mecklon.order.models.types.CreatePaymentOutboxCommandStatus;
import com.mecklon.order.models.types.OrderStatus;
import com.mecklon.order.models.types.ProductReservationDetailsStatus;
import com.mecklon.order.models.types.ReleaseProductOutboxCommandStatus;
import com.mecklon.order.redisSetup.WebsocketPublisher;
import com.mecklon.order.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderSaga {

    private final OrderRepository orderRepository;
    private final OrderTransactions orderTransactions;
    private final MongoTemplate mongoTemplate;
    private final ReservedProductEventIdempotencyKeyRepository reservedProductEventIdempotencyKeyRepository;

    @KafkaListener(topics = "${reserved-product-event}")
    @Transactional
    public void handleOrderReservedEvent(@Payload ReservedProductEvent reservedProductEvent){

        ReservedProductEventIdempotencyKey key = reservedProductEventIdempotencyKeyRepository.findById(reservedProductEvent.getOrderId()).orElse(null);
        if(key!=null)return;

        Order order = orderRepository.findById(reservedProductEvent.getOrderId()).orElse(null);

//        if(order == null){
//            // release the products;
//        }

        for(int i =0;i< order.getProducts().size();i++){
            order.getProducts().get(i).setStatus(ProductReservationDetailsStatus.RESERVED);
        }
        order.setStatus(OrderStatus.RESERVED);
        order.setTotalPrice(reservedProductEvent.getTotalPrice());
        orderRepository.save(order);

        CreatePaymentOutboxCommand command = CreatePaymentOutboxCommand.builder()
                .orderId(reservedProductEvent.getOrderId())
                .price(order.getTotalPrice())
                .status(CreatePaymentOutboxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        orderTransactions.saveCreatePaymentOutboxCommandWithIdempotencyKey(command);
    }

    private final CreatedPaymentEventIdempotencyKeyRepository createdPaymentEventIdempotencyKeyRepository;
    private final WebsocketPublisher websocketPublisher;

    @KafkaListener(topics = "${created-payment-event}")
    public void handlePaymentCreatedEvent(@Payload CreatedPaymentEvent createdPaymentEvent){
        CreatedPaymentEventIdempotencyKey key = createdPaymentEventIdempotencyKeyRepository.findById(createdPaymentEvent.getOrderId()).orElse(null);
        if(key!=null){
            return;
        }
        Order order = orderRepository.findById(createdPaymentEvent.getOrderId()).orElse(null);
        order.setPaymentId(createdPaymentEvent.getPaymentId());
        orderRepository.save(order);
        WebSocketMessage message = new WebSocketMessage("/topic/"+order.getUserId(), WebSocketEventType.ORDER_CREATED, null);
        websocketPublisher.publish(message);
        // save the payment id with the order entity and then send a websocket to the user with the payment, using redis pub sub relay and idempotency key
    }

    @KafkaListener(topics = "${failed-reserved-product-event}")
    public void handleFailedProductReservedEvents(@Payload FailedReservedProductEvent failedReservedProductEvent){
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(failedReservedProductEvent.getOrderId()));
        Update update = new Update();
        update.set("status", OrderStatus.FAILED_PRODUCT_RESERVATION);

        Order order = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Order.class
        );

        // propagate websocket failed event through redis pub-sub relay

    }

    private ReleaseProductOutboxCommandRepository releaseProductOutboxCommandRepository;
    private FailedCreatedOrderEventIdempotencyKeyRepository failedCreatedOrderEventIdempotencyKeyRepository;


    @KafkaListener(topics = "${failed-created-payment-event}")
    @Transactional
    public void handleFailedCreatePaymentEvent(@Payload FailedCreatedPaymentEvent failedCreatedPaymentEvent){
        if(failedCreatedOrderEventIdempotencyKeyRepository.existsById(failedCreatedPaymentEvent.getOrderId())){
            return;
        }
        Order order = orderRepository.findById(failedCreatedPaymentEvent.getOrderId()).orElse(null);
        if(order==null)return;

        ReleaseProductOutboxCommand command = ReleaseProductOutboxCommand.builder()
                .status(ReleaseProductOutboxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .orderId(order.getOrderId())
                .productList(order.getProducts().stream().map(product->{
                    return new ProductDetailsDTO(
                            product.getProductId(),
                            product.getQuantity()
                    );
                }).toList())
                .build();

        releaseProductOutboxCommandRepository.save(command);
        failedCreatedOrderEventIdempotencyKeyRepository.save(new FailedCreatedOrderEventIdempotencykey(failedCreatedPaymentEvent.getOrderId()));
    }

    @KafkaListener(topics = "${released-product-event}")
    public void handleReleasedProductEvent(@Payload ReleasedProductEvent releasedProductEvent){
        // multiple consumptions of the same even will lead to the same order entity status to be updated to the same thing
        // hence no idempotency key is required

        Order order = orderRepository.findById(releasedProductEvent.getOrderId()).orElse(null);
        order.setStatus(OrderStatus.FAILED_ORDERID_CREATION);
        orderRepository.save(order);

        // propagate through a redis pubsub relay with websocket to the user the failure message
    }
}
