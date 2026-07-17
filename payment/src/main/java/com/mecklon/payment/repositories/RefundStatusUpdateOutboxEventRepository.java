package com.mecklon.payment.repositories;


import com.mecklon.payment.models.RefundStatusUpdateOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundStatusUpdateOutboxEventRepository extends MongoRepository<RefundStatusUpdateOutboxEvent, String> {
}
