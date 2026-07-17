package com.mecklon.order;


import com.mecklon.core.dtos.ProductReservationInfoDTO;
import com.mecklon.core.events.*;
import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.order.dtos.WebSocketMessage;
import com.mecklon.order.dtos.types.WebSocketEventType;
import com.mecklon.order.models.*;
import com.mecklon.order.models.types.*;
import com.mecklon.order.redisSetup.WebsocketPublisher;
import com.mecklon.order.repositories.*;
import com.mongodb.DuplicateKeyException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderSaga {
    private final RedisMessageListenerContainer container;

    @PostConstruct
    public void init() {
        System.out.println(container.isRunning());
    }
    private final WebsocketPublisher websocketPublisher;
    private final OrderRepository orderRepository;
    private final OrderTransactions orderTransactions;
    private final MongoTemplate mongoTemplate;
    private final ReservedProductEventIdempotencyKeyRepository reservedProductEventIdempotencyKeyRepository;

    @KafkaListener(topics = "${reserved-product-event}")
    public void handleOrderReservedEvent(@Payload ReservedProductEvent reservedProductEvent){
        System.out.println("=============");
        System.out.println("products reserved");
        System.out.println(reservedProductEvent.getOrderId());
        System.out.println("=============");


        Order order = orderRepository.findById(reservedProductEvent.getOrderId()).orElse(null);

//        if(order == null){
//            // release the products;
//        }

        order.setStatus(OrderStatus.RESERVED);
        order.setTotalPrice(reservedProductEvent.getTotalPrice());
        order.setCreatedAt(Instant.now());
        order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.PRODUCT_RESERVED_EVENT));


        CreatePaymentOutboxCommand command = CreatePaymentOutboxCommand.builder()
                .orderId(reservedProductEvent.getOrderId())
                .price(order.getTotalPrice())
                .status(CreatePaymentOutboxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        orderTransactions.saveCreatePaymentOutboxCommandWithIdempotencyKey(order,command,new ReservedProductEventIdempotencyKey(reservedProductEvent.getOrderId()));
        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.PRODUCTS_RESERVED
                ,null);
    }

    private final CreatedPaymentEventIdempotencyKeyRepository createdPaymentEventIdempotencyKeyRepository;

    @Value("${razorpay.key-id}")
    private String razorpayApiKey;

    @KafkaListener(topics = "${created-payment-event}")
    @Transactional
    public void handlePaymentCreatedEvent(@Payload CreatedPaymentEvent createdPaymentEvent){
        createdPaymentEventIdempotencyKeyRepository.insert(new CreatedPaymentEventIdempotencyKey(createdPaymentEvent.getOrderId()));

        System.out.println("=============");
        System.out.println("razor pay id created");
        System.out.println(createdPaymentEvent.getOrderId());
        System.out.println("=============");

        Order order = orderRepository.findById(createdPaymentEvent.getOrderId()).orElse(null);
        order.setPaymentId(createdPaymentEvent.getPaymentId());
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.CREATED_PAYMENT_EVENT));

        orderRepository.save(order);

        System.out.println("publishing");
        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.ORDER_CREATED,
                Map.of(
                        "razorPayOrderId",
                        createdPaymentEvent.getRazorPayOrderId(),
                        "razorpayApiKey",
                        razorpayApiKey
                )
        );
        websocketPublisher.publish(message);
        System.out.println("after publishing");
        // save the payment id with the order entity and then send a websocket to the user with the payment, using redis pub sub relay and idempotency key
    }

    @KafkaListener(topics = "${failed-reserved-product-event}")
    public void handleFailedProductReservedEvents(@Payload FailedReservedProductEvent failedReservedProductEvent){
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(failedReservedProductEvent.getOrderId()));
        Update update = new Update();
        update.set("status", OrderStatus.FAILED_PRODUCT_RESERVATION);
        update.push("history", new SagaEventHistory(Instant.now(), true, SagaEventHistoryStatus.FAILED_PRODUCT_RESERVED_EVENT));
        Order order = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Order.class
        );

        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.FAILED_PRODUCT_RESERVATION
                ,null);
        websocketPublisher.publish(message);

    }

    private ReleaseProductOutboxCommandRepository releaseProductOutboxCommandRepository;
    private FailedCreatedOrderEventIdempotencyKeyRepository failedCreatedOrderEventIdempotencyKeyRepository;


    @KafkaListener(topics = "${failed-created-payment-event}")
    @Transactional
    public void handleFailedCreatePaymentEvent(@Payload FailedCreatedPaymentEvent failedCreatedPaymentEvent){
        failedCreatedOrderEventIdempotencyKeyRepository.insert(new FailedCreatedOrderEventIdempotencykey(failedCreatedPaymentEvent.getOrderId()));

        Order order = orderRepository.findById(failedCreatedPaymentEvent.getOrderId()).orElse(null);
        if(order==null)return;

        ReleaseProductOutboxCommand command = ReleaseProductOutboxCommand.builder()
                .status(ReleaseProductOutboxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .paymentExpired(false)
                .orderId(order.getOrderId())
                .productList(order.getProducts().stream().map(product->{
                    return new ProductReservationInfoDTO(
                            product.getProductId(),
                            product.getQuantity()
                    );
                }).toList())
                .build();

        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.FAILED_ORDER_ID_CREATION
                ,null);
        websocketPublisher.publish(message);
        order.setStatus(OrderStatus.FAILED_ORDERID_CREATION);
        order.getHistory().add(new SagaEventHistory(Instant.now(), true, SagaEventHistoryStatus.FAILED_CREATE_ORDER_ID_EVENT));
        orderRepository.save(order);
        releaseProductOutboxCommandRepository.save(command);
    }

    @KafkaListener(topics = "${released-product-event}")
    public void handleReleasedProductEvent(@Payload ReleasedProductEvent releasedProductEvent){
        // multiple consumptions of the same even will lead to the same order entity status to be updated to the same thing
        // hence no idempotency key is required

        Order order = orderRepository.findById(releasedProductEvent.getOrderId()).orElse(null);
        order.getHistory().add(new SagaEventHistory(Instant.now(), true, SagaEventHistoryStatus.RELEASED_PRODUCT_EVENT));

        orderRepository.save(order);

    }

    @KafkaListener(topics = "${successful-payment-event}")
    public void handlePaymentSuccessful(@Payload PaymentSuccessfulEvent paymentSuccessfulEvent){
        System.out.println("=============");
        System.out.println("got payment successfull");
        System.out.println(paymentSuccessfulEvent.getOrderId());
        System.out.println("=============");

        Order order = orderRepository.findById(paymentSuccessfulEvent.getOrderId()).orElse(null);
        order.setStatus(OrderStatus.BOOKED);
        order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.PAYMENT_SUCCESSFUL_EVENT));

        orderRepository.save(order);


        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.PAYMENT_CONFIRMED
                ,null);
        websocketPublisher.publish(message);
    }


    @KafkaListener(topics =  "${expired-payment-event}")
    @Transactional
    public void handlePaymentExpiredEvent(@Payload PaymentExpiredEvent paymentExpiredEvent){
        failedCreatedOrderEventIdempotencyKeyRepository.insert(new FailedCreatedOrderEventIdempotencykey(paymentExpiredEvent.getOrderId()));

        System.out.println("=============");
        System.out.println("got payment expired");
        System.out.println(paymentExpiredEvent.getOrderId());
        System.out.println("=============");

        Order order = orderRepository.findById(paymentExpiredEvent.getOrderId()).orElse(null);
        order.setStatus(OrderStatus.PAYMENT_EXPIRED);
        order.getHistory().add(new SagaEventHistory(Instant.now(), true, SagaEventHistoryStatus.EXPIRED_PAYMENT_EVENT));



        ReleaseProductOutboxCommand command = ReleaseProductOutboxCommand.builder()
                .status(ReleaseProductOutboxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .orderId(order.getOrderId())
                .paymentExpired(true)
                .productList(order.getProducts().stream().map(product->{
                    return new ProductReservationInfoDTO(
                            product.getProductId(),
                            product.getQuantity()
                    );
                }).toList())
                .build();

        orderRepository.save(order);

        releaseProductOutboxCommandRepository.save(command);
        WebSocketMessage message = new WebSocketMessage(
                "/topic/room/"+order.getCheckoutSessionId()
                , WebSocketEventType.PAYMENT_EXPIRED
                ,null);
        websocketPublisher.publish(message);
    }


    @KafkaListener(topics="${payment-refund-status-update}")
    @Transactional
    public void handlePaymentRefundUpdate(@Payload PaymentRefundStatusUpdateEvent paymentRefundStatusUpdateEvent){
        Order order = orderRepository.findById(paymentRefundStatusUpdateEvent.getOrderId()).orElse(null);
        if(paymentRefundStatusUpdateEvent.getRefundSuccessful()){
            order.setStatus(OrderStatus.REFUNDED);
            order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.REFUNDED_EVENT));

        }else{
            order.setStatus(OrderStatus.REFUND_FAILED);
            order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.REFUND_FAILED_EVENT));

        }
        orderRepository.save(order);
    }
}
