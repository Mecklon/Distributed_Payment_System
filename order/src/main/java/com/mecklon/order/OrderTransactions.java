package com.mecklon.order;


import com.mecklon.order.models.*;
import com.mecklon.order.models.types.PropagateReserveOrderOutBoxCommandStatus;
import com.mecklon.order.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderTransactions {

    private final CreatePaymentOutBoxCommandRepository createPaymentOutBoxCommandRepository;
    private final ReservedProductEventIdempotencyKeyRepository reservedProductEventIdempotencyKeyRepository;


    @Transactional
    public void saveCreatePaymentOutboxCommandWithIdempotencyKey(Order order, CreatePaymentOutboxCommand command, ReservedProductEventIdempotencyKey reservedProductEventIdempotencyKey){
        reservedProductEventIdempotencyKeyRepository.insert(reservedProductEventIdempotencyKey);

        orderRepository.save(order);
        createPaymentOutBoxCommandRepository.save(command);
    }

    private final OrderRepository orderRepository;
    private final PropagateReserveOrderOutboxCommandRepository propagateReserveOrderOutboxCommandRepository;
    private final CreateOrderIdempotencyKeyRepository createOrderIdempotencyKeyRepository;

    @Transactional
    public void saveOrderWithOutbox(Order order){
        createOrderIdempotencyKeyRepository.insert(new CreateOrderIdempotencyKey(order.getCheckoutSessionId()));

        System.out.println("should print once");
        Order savedOrder = orderRepository.save(order);
        PropagateReserveOrderOutboxCommand outBoxCommand = PropagateReserveOrderOutboxCommand.builder()
                .orderId(order.getOrderId())
                .status(PropagateReserveOrderOutBoxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        System.out.println("Order id: "+savedOrder.getOrderId());
        propagateReserveOrderOutboxCommandRepository.save(outBoxCommand);
    }
}
