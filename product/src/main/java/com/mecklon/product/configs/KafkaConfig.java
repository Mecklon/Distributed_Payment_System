package com.mecklon.product.configs;

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
    NewTopic failedCreateReserveProductCommandTopicDLT() {
        return TopicBuilder.name(failedReservedProductEventTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReserveProductCommandTopic() {
        return TopicBuilder.name(reservedProductEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReserveProductCommandTopicDLT() {
        return TopicBuilder.name(reservedProductEventTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleasedProductEvent() {
        return TopicBuilder.name(releasedProductEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createReleasedProductEventDLT() {
        return TopicBuilder.name(releasedProductEventTopic+".DLT").partitions(3).replicas(3).build();
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
