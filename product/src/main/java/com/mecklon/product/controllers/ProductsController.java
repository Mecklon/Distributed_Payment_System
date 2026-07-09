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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductService productService;

    @PostMapping("/addProduct")
    public ResponseEntity<Object> saveProduct(MultipartFile profile,@RequestBody AddProductRequest request){
        try{
            productService.saveProduct(profile, request);
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
