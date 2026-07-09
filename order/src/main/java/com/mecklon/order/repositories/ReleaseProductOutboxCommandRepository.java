package com.mecklon.order.repositories;


import com.mecklon.order.models.ReleaseProductOutboxCommand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseProductOutboxCommandRepository extends MongoRepository<ReleaseProductOutboxCommand, String> {
}
