package com.resumeai.export.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void resumeaiExchange_createsTopicExchange() {
        ReflectionTestUtils.setField(config, "exchange", "resumeai.exchange");

        TopicExchange exchange = config.resumeaiExchange();

        assertNotNull(exchange);
        assertEquals("resumeai.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    void jsonMessageConverter_returnsJacksonConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    void rabbitTemplate_usesJsonMessageConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        assertNotNull(template);
        assertInstanceOf(Jackson2JsonMessageConverter.class, template.getMessageConverter());
    }
}
