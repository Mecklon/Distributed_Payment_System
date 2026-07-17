package com.mecklon.product.controllers;


import com.fasterxml.jackson.core.io.IOContext;
import com.mecklon.core.dtos.ErrorResponse;
import com.mecklon.product.dtos.AddProductRequest;
import com.mecklon.product.dtos.ProductDTO;
import com.mecklon.product.dtos.ProductDetails;
import com.mecklon.product.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;


@RestController
@RequiredArgsConstructor
public class ProductsController {

    private final ProductService productService;

    @PostMapping("/addProduct")
    public ResponseEntity<Object> saveProduct(@RequestParam(name = "profile", required = false) MultipartFile profile,
    @RequestParam(name = "name", required = false) String name,
    @RequestParam(name = "rating", required = false) Double rating,
    @RequestParam(name = "description", required = false) String description,
    @RequestParam(name = "category", required = false) String category,
    @RequestParam(name = "price", required = false) Double price,
    @RequestParam(name = "stock", required = false) Long stock

    ){
        System.out.println(name);
        try{
            productService.saveProduct(profile, new AddProductRequest(name, rating, description, category, price, stock));
        }catch (IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("INTERNAL_SERVER_ERROR","Could not save the provided image")
            );
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/getProduct")
    public ResponseEntity<List<ProductDTO>> getProduct(@RequestBody ProductDetails productDetails){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProducts(productDetails));
    }
}
