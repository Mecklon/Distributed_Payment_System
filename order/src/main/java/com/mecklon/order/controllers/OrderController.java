package com.mecklon.order.controllers;


import com.mecklon.order.dtos.CreateOrderRequest;
import com.mecklon.order.dtos.OrderDTO;
import com.mecklon.order.services.OrderService;

import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/book")
    public ResponseEntity<Void> bookOrder(Authentication auth, @RequestBody CreateOrderRequest createOrderRequest){
        orderService.bookOrder(auth, createOrderRequest);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/getHistory")
    public ResponseEntity<List<OrderDTO>> getHistory(Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getHistory(auth));
    }

}
