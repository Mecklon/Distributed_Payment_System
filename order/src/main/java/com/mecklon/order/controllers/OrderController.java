package com.mecklon.order.controllers;


import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/book")
    public ResponseEntity<Void> bookOrder(Authentication auth, @RequestBody List<ProductDetailsDTO> products){
        orderService.bookOrder(auth, products);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
