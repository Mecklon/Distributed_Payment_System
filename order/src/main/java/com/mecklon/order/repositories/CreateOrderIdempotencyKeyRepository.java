package com.mecklon.order.repositories;


import com.mecklon.order.models.CreateOrderIdempotencyKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreateOrderIdempotencyKeyRepository extends MongoRepository<CreateOrderIdempotencyKey, String> {
}
