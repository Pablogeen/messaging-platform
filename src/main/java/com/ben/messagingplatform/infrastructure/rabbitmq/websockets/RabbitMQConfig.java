
package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MESSAGING_EXCHANGE = "messaging.events";
    public static final String MESSAGE_CREATED_QUEUE = "message.created.queue";

    public static final String DEAD_LETTER_EXCHANGE = "messaging.dlx";
    public static final String DEAD_LETTER_QUEUE = "message.created.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "message.created.failed";


    public static final String ROUTING_DEAD_LETTER_QUEUE = "message.routing.dlq";
    public static final String ROUTING_DEAD_LETTER_KEY = "message.routing.failed";

    @Bean
    public TopicExchange messagingExchange() {
        return new TopicExchange(MESSAGING_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue messageCreatedQueue() {
        return new Queue(
                MESSAGE_CREATED_QUEUE,
                true,
                false,
                false,
                java.util.Map.of(
                        "x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
                        "x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY
                )
        );
    }

    @Bean
    public Queue messageCreatedDeadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding messageCreatedBinding(
            @Qualifier("messageCreatedQueue") Queue queue,
            @Qualifier("messagingExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with("message.created");
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("messageCreatedDeadLetterQueue") Queue queue,
            @Qualifier("deadLetterExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue routingDeadLetterQueue() {
        return new Queue(ROUTING_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding routingDeadLetterBinding(
            @Qualifier("routingDeadLetterQueue") Queue queue,
            @Qualifier("deadLetterExchange") TopicExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_DEAD_LETTER_KEY);
    }
}
