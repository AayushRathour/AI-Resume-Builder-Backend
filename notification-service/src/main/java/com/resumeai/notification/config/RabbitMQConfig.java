package com.resumeai.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.export}")
    private String exportQueue;

    @Value("${rabbitmq.queue.ai}")
    private String aiQueue;

    @Value("${rabbitmq.queue.job}")
    private String jobQueue;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    // ─── Queues ──────────────────────────────────────────────────

    @Bean
    public Queue exportCompletedQueue() {
        return QueueBuilder.durable(exportQueue).build();
    }

    @Bean
    public Queue aiCompletedQueue() {
        return QueueBuilder.durable(aiQueue).build();
    }

    @Bean
    public Queue jobMatchQueue() {
        return QueueBuilder.durable(jobQueue).build();
    }

    // ─── Exchange ─────────────────────────────────────────────────

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchange);
    }

    // ─── Bindings ─────────────────────────────────────────────────

    @Bean
    public Binding exportBinding(Queue exportCompletedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(exportCompletedQueue).to(notificationExchange).with("export.#");
    }

    @Bean
    public Binding aiBinding(Queue aiCompletedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(aiCompletedQueue).to(notificationExchange).with("ai.#");
    }

    @Bean
    public Binding jobBinding(Queue jobMatchQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(jobMatchQueue).to(notificationExchange).with("job.#");
    }

    // ─── JSON Message Converter ───────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
