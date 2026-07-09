package com.mecklon.product.services;


import com.mecklon.product.dtos.AddProductRequest;
import com.mecklon.product.dtos.ProductDTO;
import com.mecklon.product.dtos.ProductDetails;
import com.mecklon.product.model.Product;
import com.mecklon.product.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ProductRepository productRepository;

    public void saveProduct(MultipartFile profile, AddProductRequest request) throws IOException {
        Product newProduct = new Product();
        newProduct.setCategory(request.getCategory());
        newProduct.setName(request.getName());
        newProduct.setDescription(request.getDescription());
        newProduct.setPrice(request.getPrice());
        newProduct.setStock(request.getStock());
        newProduct.setRating(request.getRating());


        if(profile!=null){
            String uniqueName = System.currentTimeMillis() + "-" + profile.getOriginalFilename();
            String path = uploadDir + File.separator + uniqueName;
            profile.transferTo(new File(path));
            newProduct.setImgUrl(path);
            newProduct.setImgName(uniqueName);
        }

        productRepository.save(newProduct);
    }

    public List<ProductDTO> getProducts(ProductDetails productDetails) {
        if(productDetails.getCategory()==null){
            return productRepository.findByNameStartingWithIgnoreCase(productDetails.getName());
        }
        return productRepository.findByNameStartingWithIgnoreCaseAndCategory(productDetails.getName(), productDetails.getCategory());
    }
}
