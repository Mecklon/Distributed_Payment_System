package com.mecklon.order.models;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedCreatedOrderEventIdempotencykey {
    @Id
    private String key;
}
