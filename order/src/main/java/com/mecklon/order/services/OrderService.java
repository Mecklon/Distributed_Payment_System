package com.mecklon.order.services;


import com.mecklon.core.security.JwtPrincipal;
import com.mecklon.order.OrderTransactions;
import com.mecklon.order.dtos.CreateOrderRequest;
import com.mecklon.order.dtos.OrderDTO;
import com.mecklon.order.models.Order;
import com.mecklon.order.models.ProductReservationDetails;
import com.mecklon.order.models.SagaEventHistory;
import com.mecklon.order.models.types.OrderStatus;
import com.mecklon.order.models.types.ProductReservationDetailsStatus;
import com.mecklon.order.models.types.SagaEventHistoryStatus;
import com.mecklon.order.repositories.CreateOrderIdempotencyKeyRepository;
import com.mecklon.order.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderTransactions orderTransactions;
    private final CreateOrderIdempotencyKeyRepository createOrderIdempotencyKeyRepository;
    private final OrderRepository orderRepository;


    public void bookOrder(Authentication auth, CreateOrderRequest createOrderRequest) {
        System.out.println("booking");
        JwtPrincipal principal = (JwtPrincipal)auth.getPrincipal();

        Order order = Order.builder()
                .userId(principal.getUserId())
                .status(OrderStatus.CREATED)
                .checkoutSessionId(createOrderRequest.getCheckoutSessionId())
                .products(createOrderRequest.getProducts().stream().map(product->{
                   return new ProductReservationDetails(
                           product.getProductId(),
                           product.getName(),
                           product.getCategory(),
                           product.getPrice(),
                           product.getImgName(),
                           product.getQuantity()
                   ) ;
                }).toList())
                .build();
        order.getHistory().add(new SagaEventHistory(Instant.now(), false, SagaEventHistoryStatus.CREATED));
        System.out.println("saved order outbox");
        try {
            orderTransactions.saveOrderWithOutbox(order);
        }catch (DuplicateKeyException e){
            e.getStackTrace();
            System.out.println(e.getMessage());
            System.out.println(e.getClass().getName());
            e.getCause();
            System.out.println("as;dlfasslf");
            return;
        }
    }

    public List<OrderDTO> getHistory(Authentication auth) {
        JwtPrincipal userDetails = (JwtPrincipal) auth.getPrincipal();
        List<Order> orders = orderRepository.findByUserId(userDetails.getUserId());
        return orders.stream().map(order -> new OrderDTO(
                order.getOrderId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getProducts(),
                order.getCreatedAt(),
                order.getHistory()
        )).toList().reversed();
    }
}
