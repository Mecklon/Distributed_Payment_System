package com.mecklon.product.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {


    @Value("${reserved-product-event}")
    private String reservedProductEventTopic;

    @Value("${failed-reserved-product-event}")
    private String failedReservedProductEventTopic;


    @Value("${released-product-event}")
    private String releasedProductEventTopic;

    @Bean
    NewTopic failedCreateReserveProductCommandTopic() {
        return TopicBuilder.name(failedReservedProductEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReserveProductCommandTopic() {
        return TopicBuilder.name(reservedProductEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleasedProductEvent() {
        return TopicBuilder.name(releasedProductEventTopic).partitions(3).replicas(3).build();
    }
}
