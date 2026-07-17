package com.mecklon.payment.repositories;


import com.mecklon.payment.models.PaymentExpiredOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentExpiredOutboxEventRepository extends MongoRepository<PaymentExpiredOutboxEvent,String> {

}
