package com.mecklon.payment.repositories;

import com.mecklon.payment.models.CreatedPaymentOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatedPaymentOutboxEventRepository extends MongoRepository<CreatedPaymentOutboxEvent, String> {
}
