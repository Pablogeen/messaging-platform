package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange messagingExchange() {
        return new TopicExchange("messaging.events");
    }

    @Bean
    public Queue messageCreatedQueue() {
        return new Queue("message.created.queue", true);
    }

    @Bean
    public Binding messageCreatedBinding(Queue messageCreatedQueue, TopicExchange messagingExchange) {
        return BindingBuilder
                .bind(messageCreatedQueue)
                .to(messagingExchange)
                .with("message.created");
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
