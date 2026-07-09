package com.mecklon.order.repositories;

import com.mecklon.order.models.CreatePaymentOutboxCommand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatePaymentOutBoxCommandRepository extends MongoRepository<CreatePaymentOutboxCommand, String> {
}
