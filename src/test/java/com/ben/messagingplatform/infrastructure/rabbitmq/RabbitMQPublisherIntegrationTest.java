package com.ben.messagingplatform.infrastructure.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RabbitMQPublisherIntegrationTest {

    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldPublishJsonMessageToRabbitMQ() {

        Map<String, Object> event = Map.of(
                "messageId", 100L,
                "conversationId", 20L,
                "content", "Hello RabbitMQ"
        );

        rabbitMQPublisher.publish(event);

        Object received = rabbitTemplate.receiveAndConvert(
                "message.created.queue",
                5000
        );

        assertThat(received).isNotNull();
        assertThat(received).isInstanceOf(Map.class);

        Map<?, ?> receivedEvent = (Map<?, ?>) received;

        assertThat(receivedEvent.get("content"))
                .isEqualTo("Hello RabbitMQ");

        assertThat(receivedEvent.get("messageId"))
                .isEqualTo(100);

    }
}
