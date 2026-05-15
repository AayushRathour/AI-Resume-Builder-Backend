package com.resumeai.template.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ configuration for exchanges, queues, bindings, and routing in template-service. */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange:resumeai.exchange}")
    private String exchange;

    @Bean
    public TopicExchange resumeaiExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    @SuppressWarnings("java:S5738")
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
