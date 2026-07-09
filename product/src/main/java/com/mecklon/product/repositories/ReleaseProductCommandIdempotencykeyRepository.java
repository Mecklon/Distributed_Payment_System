package com.mecklon.product.repositories;


import com.mecklon.product.model.ReleaseProductCommandIdempotencyKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseProductCommandIdempotencykeyRepository extends MongoRepository<ReleaseProductCommandIdempotencyKey, String> {
}
