package com.mecklon.payment.repositories;

import com.mecklon.payment.models.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    boolean existsByOrderId(String orderId);

    Payment findByOrderId(String orderId);

    Payment findByPaymentId(String paymentId);
}
