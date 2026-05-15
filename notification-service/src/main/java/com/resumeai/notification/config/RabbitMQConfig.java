package com.resumeai.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ topology for notification-service event consumption.
 * Defines exchanges, queues, bindings, JSON conversion, and listener retry behavior.
 */

@Configuration
public class RabbitMQConfig {

    // Exchange name shared across producing services.
    public static final String EXCHANGE = "resumeai.exchange";

    // Queue names consumed by notification-service listeners.
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String RESUME_QUEUE = "resume.queue";
    public static final String EXPORT_QUEUE = "export.queue";
    public static final String TEMPLATE_QUEUE = "template.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    public static final String RESUME_CREATED_KEY = "resume.created";
    public static final String RESUME_UPDATED_KEY = "resume.updated";
    public static final String RESUME_EXPORTED_KEY = "resume.exported";
    public static final String RESUME_DELETED_KEY = "resume.deleted";
    public static final String TEMPLATE_CREATED_KEY = "template.created";
    public static final String TEMPLATE_UPDATED_KEY = "template.updated";
    public static final String TEMPLATE_DELETED_KEY = "template.deleted";
    public static final String JOB_APPLIED_KEY = "job.applied";
    public static final String ATS_COMPLETED_KEY = "ats.completed";
    public static final String NOTIFICATION_SEND_KEY = "notification.send";

    public static final String DLX_EXCHANGE = "resumeai.dlx";
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String DLQ_NOTIFICATION_KEY = "dlq.notification";


    @Bean
    public TopicExchange resumeaiExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE).durable(true).build();
    }


    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, DLQ_NOTIFICATION_KEY)
                .build();
    }

    @Bean
    public Queue resumeQueue() {
        return QueueBuilder.durable(RESUME_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, DLQ_NOTIFICATION_KEY)
                .build();
    }

    @Bean
    public Queue exportQueue() {
        return QueueBuilder.durable(EXPORT_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, DLQ_NOTIFICATION_KEY)
                .build();
    }

    @Bean
    public Queue templateQueue() {
        return QueueBuilder.durable(TEMPLATE_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, DLX_EXCHANGE)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, DLQ_NOTIFICATION_KEY)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }


    @Bean
    public Binding notificationSendBinding() {
        return BindingBuilder.bind(notificationQueue()).to(resumeaiExchange()).with(NOTIFICATION_SEND_KEY);
    }

    @Bean
    public Binding atsCompletedBinding() {
        return BindingBuilder.bind(notificationQueue()).to(resumeaiExchange()).with(ATS_COMPLETED_KEY);
    }

    @Bean
    public Binding jobAppliedBinding() {
        return BindingBuilder.bind(notificationQueue()).to(resumeaiExchange()).with(JOB_APPLIED_KEY);
    }


    @Bean
    public Binding resumeCreatedBinding() {
        return BindingBuilder.bind(resumeQueue()).to(resumeaiExchange()).with(RESUME_CREATED_KEY);
    }

    @Bean
    public Binding resumeUpdatedBinding() {
        return BindingBuilder.bind(resumeQueue()).to(resumeaiExchange()).with(RESUME_UPDATED_KEY);
    }

    @Bean
    public Binding resumeDeletedBinding() {
        return BindingBuilder.bind(resumeQueue()).to(resumeaiExchange()).with(RESUME_DELETED_KEY);
    }


    @Bean
    public Binding resumeExportedBinding() {
        return BindingBuilder.bind(exportQueue()).to(resumeaiExchange()).with(RESUME_EXPORTED_KEY);
    }


    @Bean
    public Binding templateCreatedBinding() {
        return BindingBuilder.bind(templateQueue()).to(resumeaiExchange()).with(TEMPLATE_CREATED_KEY);
    }

    @Bean
    public Binding templateUpdatedBinding() {
        return BindingBuilder.bind(templateQueue()).to(resumeaiExchange()).with(TEMPLATE_UPDATED_KEY);
    }

    @Bean
    public Binding templateDeletedBinding() {
        return BindingBuilder.bind(templateQueue()).to(resumeaiExchange()).with(TEMPLATE_DELETED_KEY);
    }


    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with("dlq.#");
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(EXCHANGE);
        return template;
    }


    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        // Retry configuration
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOff);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        factory.setAdviceChain(
                org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                        .stateless()
                        .retryOperations(retryTemplate)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build()
        );

        return factory;
    }
}
    // Routing keys mapped from producer services.
    // Dead-letter topology for failed event processing.
    // Exchange bean registrations.
    // Queue bean registrations.
    // Binding registrations for each event category.
    // JSON message conversion and RabbitTemplate defaults.
    // Listener factory with retry and dead-letter recovery behavior.
