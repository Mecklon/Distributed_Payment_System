package com.mecklon.payment.repositories;


import com.mecklon.payment.models.PaymentSuccessfulEventOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSuccessfulEventOutboxEventRepository extends MongoRepository<PaymentSuccessfulEventOutboxEvent, String> {
}
