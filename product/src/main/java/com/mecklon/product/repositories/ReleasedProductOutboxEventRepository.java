package com.mecklon.product.repositories;

import com.mecklon.product.model.ReleasedProductOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleasedProductOutboxEventRepository extends MongoRepository<ReleasedProductOutboxEvent,String> {
}
