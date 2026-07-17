package com.mecklon.order.configs;

import org.springframework.dao.DuplicateKeyException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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
    NewTopic createReserveProductCommandTopicDLT() {
        return TopicBuilder.name(reserveProductCommandTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createCreatePaymentCommandTopic(){
        return TopicBuilder.name(createPaymentCommandTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createCreatePaymentCommandTopicDLT(){
        return TopicBuilder.name(createPaymentCommandTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleaseProductCommandTopic(){
        return TopicBuilder.name(releaseProductCommandTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleaseProductCommandTopicDLT(){
        return TopicBuilder.name(releaseProductCommandTopic+".DLT").partitions(3).replicas(3).build();
    }


    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> {
                    if (ex instanceof DuplicateKeyException) {
                        // Already processed, acknowledge and ignore.
                        return;
                    }

                    // For all other exceptions, send to DLT.
                    new DeadLetterPublishingRecoverer(kafkaTemplate)
                            .accept(record, ex);
                },
                new FixedBackOff(5000L, 3)
        );

        errorHandler.addNotRetryableExceptions(DuplicateKeyException.class);

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
