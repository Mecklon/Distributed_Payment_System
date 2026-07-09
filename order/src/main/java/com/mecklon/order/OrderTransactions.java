package com.mecklon.order;


import com.mecklon.order.models.CreatePaymentOutboxCommand;
import com.mecklon.order.models.Order;
import com.mecklon.order.models.PropagateReserveOrderOutboxCommand;
import com.mecklon.order.models.ReservedProductEventIdempotencyKey;
import com.mecklon.order.models.types.PropagateReserveOrderOutBoxCommandStatus;
import com.mecklon.order.repositories.CreatePaymentOutBoxCommandRepository;
import com.mecklon.order.repositories.OrderRepository;
import com.mecklon.order.repositories.PropagateReserveOrderOutboxCommandRepository;
import com.mecklon.order.repositories.ReservedProductEventIdempotencyKeyRepository;
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
    public void saveCreatePaymentOutboxCommandWithIdempotencyKey(CreatePaymentOutboxCommand command){

        reservedProductEventIdempotencyKeyRepository.save(new ReservedProductEventIdempotencyKey(command.getOrderId()));
        createPaymentOutBoxCommandRepository.save(command);
    }

    private final OrderRepository orderRepository;
    private final PropagateReserveOrderOutboxCommandRepository propagateReserveOrderOutboxCommandRepository;

    @Transactional
    public void saveOrderWithOutbox(Order order){
        Order savedOrder = orderRepository.save(order);
        PropagateReserveOrderOutboxCommand outBoxCommand = PropagateReserveOrderOutboxCommand.builder()
                .orderId(order.getOrderId())
                .status(PropagateReserveOrderOutBoxCommandStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        propagateReserveOrderOutboxCommandRepository.save(outBoxCommand);
    }



}
