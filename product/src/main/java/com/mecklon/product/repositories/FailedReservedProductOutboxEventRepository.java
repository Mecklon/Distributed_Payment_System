package com.mecklon.product.repositories;


import com.mecklon.product.model.FailedReservedProductOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedReservedProductOutboxEventRepository extends MongoRepository<FailedReservedProductOutboxEvent, String> {
}
