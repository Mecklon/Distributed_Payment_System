package com.mecklon.payment.configurations;


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

    @Value("${created-payment-event}")
    private String createdPaymentEvent;


    @Value("${failed-created-payment-event}")
    private String failedCreatedPaymentEvent;

    @Value("${successful-payment-event}")
    private String successfulPaymentEventTopic;


    @Value("${expired-payment-event}")
    private String expiredPaymentEventTopic;

    @Value("${payment-refund-status-update}")
    private String paymentRefundStatusUpdateTopic;



    @Bean
    NewTopic createCreatedPaymentEvent() {
        return TopicBuilder.name(createdPaymentEvent).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createCreatedPaymentEventDLT() {
        return TopicBuilder.name(createdPaymentEvent+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createFailedCreatedPaymentEvent() {
        return TopicBuilder.name(createdPaymentEvent).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createFailedCreatedPaymentEventDLT() {
        return TopicBuilder.name(createdPaymentEvent+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createSuccessfulPaymentEvent() {
        return TopicBuilder.name(successfulPaymentEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createSuccessfulPaymentEventDLT() {
        return TopicBuilder.name(successfulPaymentEventTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createExpiredPaymentEvent() {
        return TopicBuilder.name(expiredPaymentEventTopic).partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createExpiredPaymentEventDLT() {
        return TopicBuilder.name(expiredPaymentEventTopic+".DLT").partitions(3).replicas(3).build();
    }

    @Bean
    NewTopic createPaymentRefundStatusUpdateTopic() {
        return TopicBuilder.name(paymentRefundStatusUpdateTopic).partitions(3).replicas(3).build();
    }


    @Bean
    NewTopic createPaymentRefundStatusUpdateTopicDLT() {
        return TopicBuilder.name(paymentRefundStatusUpdateTopic+"DLT").partitions(3).replicas(3).build();
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
