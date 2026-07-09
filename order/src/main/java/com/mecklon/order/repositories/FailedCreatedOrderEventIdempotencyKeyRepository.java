package com.mecklon.order.repositories;


import com.mecklon.order.models.FailedCreatedOrderEventIdempotencykey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedCreatedOrderEventIdempotencyKeyRepository extends MongoRepository<FailedCreatedOrderEventIdempotencykey, String> {
}
