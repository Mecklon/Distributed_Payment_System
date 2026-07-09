package com.mecklon.order.repositories;


import com.mecklon.order.models.CreatedPaymentEventIdempotencyKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatedPaymentEventIdempotencyKeyRepository extends MongoRepository<CreatedPaymentEventIdempotencyKey, String> {
}
