package com.ben.messagingplatform.infrastructure.rabbitmq;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.ServerIdentity;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageRoutingConfig {

    public static final String MESSAGE_ROUTING_EXCHANGE =
            "message.routing.exchange";

    public static final String SERVER_ROUTING_KEY_PREFIX = "server.";

    private final ServerIdentity serverIdentity;

    public MessageRoutingConfig(ServerIdentity serverIdentity) {
        this.serverIdentity = serverIdentity;
    }

    @Bean
    public TopicExchange messageRoutingExchange() {
        return new TopicExchange(
                MESSAGE_ROUTING_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue messageRoutingQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding messageRoutingBinding(
            @Qualifier("messageRoutingQueue") Queue messageRoutingQueue,
            @Qualifier("messageRoutingExchange") TopicExchange messageRoutingExchange
    ) {
        String routingKey =
                SERVER_ROUTING_KEY_PREFIX + serverIdentity.getServerId();

        return BindingBuilder
                .bind(messageRoutingQueue)
                .to(messageRoutingExchange)
                .with(routingKey);
    }
}