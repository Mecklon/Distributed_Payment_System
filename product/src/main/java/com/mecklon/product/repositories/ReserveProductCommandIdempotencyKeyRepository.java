package com.mecklon.product.repositories;

import com.mecklon.product.model.ReserveProductCommandIdempotencyKey;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReserveProductCommandIdempotencyKeyRepository  extends MongoRepository<ReserveProductCommandIdempotencyKey, String> {
}
