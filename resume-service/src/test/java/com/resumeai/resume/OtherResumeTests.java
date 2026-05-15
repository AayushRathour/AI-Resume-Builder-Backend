package com.resumeai.resume;

import com.resumeai.resume.config.RabbitMQConfig;
import com.resumeai.resume.controller.ServiceStatusController;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OtherResumeTests {

    @Test
    void serviceStatusController_Root() {
        ServiceStatusController c = new ServiceStatusController();
        ResponseEntity<Map<String, String>> r = c.root();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("resume-service", r.getBody().get("service"));
    }

    @Test
    void serviceStatusController_Health() {
        ServiceStatusController c = new ServiceStatusController();
        ResponseEntity<Map<String, String>> r = c.health();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("UP", r.getBody().get("status"));
    }

    @Test
    void rabbitMQConfig_Exchange() {
        RabbitMQConfig config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchange", "resumeai.exchange");
        TopicExchange exchange = config.resumeaiExchange();
        assertNotNull(exchange);
        assertEquals("resumeai.exchange", exchange.getName());
    }

    @Test
    void rabbitMQConfig_MessageConverter() {
        RabbitMQConfig config = new RabbitMQConfig();
        MessageConverter converter = config.jsonMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(JacksonJsonMessageConverter.class, converter);
    }

    @Test
    void rabbitMQConfig_RabbitTemplate() {
        RabbitMQConfig config = new RabbitMQConfig();
        ConnectionFactory cf = mock(ConnectionFactory.class);
        RabbitTemplate template = config.rabbitTemplate(cf);
        assertNotNull(template);
        assertInstanceOf(JacksonJsonMessageConverter.class, template.getMessageConverter());
    }
}
