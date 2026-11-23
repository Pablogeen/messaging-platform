package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;


import com.ben.messagingplatform.message.event.MessageCreatedEvent;
import com.ben.messagingplatform.routing.dto.service.MessageRoutingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class MessageCreatedEventConsumer {

    private final MessageRoutingOrchestrator routingOrchestrator;

    @RabbitListener(queues = "message.created.queue")
    public void consume(
            MessageCreatedEvent event,
            @Header(AmqpHeaders.MESSAGE_ID) String eventId
    ) {

        log.info(
                "Processing message-created event: {}",
                event.messageId()
        );

        routingOrchestrator.routeMessage(event, eventId);
    }
}

