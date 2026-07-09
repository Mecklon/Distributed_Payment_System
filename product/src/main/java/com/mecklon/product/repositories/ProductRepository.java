package com.mecklon.product.repositories;


import com.mecklon.product.dtos.ProductDTO;
import com.mecklon.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<ProductDTO> findByNameStartingWithIgnoreCase(String name);

    List<ProductDTO> findByNameStartingWithIgnoreCaseAndCategory(
            String name,
            String category
    );

}
