package com.mecklon.order.services;


import com.mecklon.core.security.JwtPrincipal;
import com.mecklon.order.OrderTransactions;
import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.order.models.Order;
import com.mecklon.order.models.ProductReservationDetails;
import com.mecklon.order.models.types.OrderStatus;
import com.mecklon.order.models.types.ProductReservationDetailsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderTransactions orderTransactions;

    public void bookOrder(Authentication auth, List<ProductDetailsDTO> products) {

        JwtPrincipal principal = (JwtPrincipal)auth.getPrincipal();

        Order order = Order.builder()
                .userId(principal.getUserId())
                .status(OrderStatus.CREATED)
                .products(products.stream().map(product->{
                   return new ProductReservationDetails(
                           product.getProductId(),
                           product.getQuantity(),
                           ProductReservationDetailsStatus.PENDING
                   ) ;
                }).toList())
                .build();

        orderTransactions.saveOrderWithOutbox(order);
    }
}
