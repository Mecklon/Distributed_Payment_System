package com.mecklon.payment.repositories;


import com.mecklon.payment.models.CheckPaymentStatusOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckPaymentStatusOutboxEventRepository extends MongoRepository<CheckPaymentStatusOutboxEvent,String> {
}
