package com.mecklon.product.repositories;

import com.mecklon.product.model.ReservedProductOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservedProductOutBoxEventRepository extends MongoRepository<ReservedProductOutboxEvent,String > {
}
