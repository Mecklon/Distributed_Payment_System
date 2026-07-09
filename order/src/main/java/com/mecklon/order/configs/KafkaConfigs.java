package com.mecklon.order.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfigs {

    @Value("${reserve-product-command}")
    private String reserveProductCommandTopic;

    @Value("${create-payment-command}")
    private String createPaymentCommandTopic;

    @Value("${release-product-command}")
    private String releaseProductCommandTopic;



    @Bean
    NewTopic createReserveProductCommandTopic() {
        return TopicBuilder.name(reserveProductCommandTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createCreatePaymentCommandTopic(){
        return TopicBuilder.name(createPaymentCommandTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleaseProductCommandTopic(){
        return TopicBuilder.name(releaseProductCommandTopic).partitions(3).replicas(3).build();
    }
}
