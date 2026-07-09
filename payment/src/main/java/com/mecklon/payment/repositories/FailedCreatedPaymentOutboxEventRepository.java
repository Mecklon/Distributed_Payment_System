package com.mecklon.payment.repositories;


import com.mecklon.payment.models.FailedCreatedPaymentOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedCreatedPaymentOutboxEventRepository extends MongoRepository<FailedCreatedPaymentOutboxEvent , String> {
}
