package com.mecklon.payment.repositories;


import com.mecklon.payment.models.GetOrderIdOutboxRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GetOrderIdOutBoxRequestRepository extends MongoRepository<GetOrderIdOutboxRequest, String> {
}
