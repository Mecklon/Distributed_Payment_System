package com.mecklon.order.repositories;


import com.mecklon.order.models.PropagateReserveOrderOutboxCommand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropagateReserveOrderOutboxCommandRepository extends MongoRepository<PropagateReserveOrderOutboxCommand, String> {
}
