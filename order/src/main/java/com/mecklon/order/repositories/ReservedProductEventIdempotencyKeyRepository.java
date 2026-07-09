package com.mecklon.order.repositories;


import com.mecklon.core.events.ReservedProductEvent;
import com.mecklon.order.models.ReservedProductEventIdempotencyKey;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ReservedProductEventIdempotencyKeyRepository extends MongoRepository<ReservedProductEventIdempotencyKey,String> {


}
