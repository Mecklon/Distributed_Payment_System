package com.mecklon.payment.configurations;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfigs {

    @Value("${created-payment-event}")
    private String createdPaymentEvent;


    @Value("${failed-created-payment-event}")
    private String failedCreatedPaymentEvent;



    @Bean
    NewTopic createCreatedPaymentEvent() {
        return TopicBuilder.name(createdPaymentEvent).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createFailedCreatedPaymentEvent() {
        return TopicBuilder.name(createdPaymentEvent).partitions(3).replicas(3).build();
    }
}
