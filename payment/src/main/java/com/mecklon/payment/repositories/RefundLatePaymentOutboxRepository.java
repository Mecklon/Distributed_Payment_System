package com.mecklon.payment.repositories;


import com.mecklon.payment.models.RefundLatePaymentOutbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundLatePaymentOutboxRepository extends MongoRepository<RefundLatePaymentOutbox, String> {
}
