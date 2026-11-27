package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationSubscriptionInterceptor implements ChannelInterceptor {

    private final ConversationParticipantRepository participantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();

        if (destination == null ||
                !destination.matches("^/topic/conversations/\\d+/receipts$")) {
            return message;
        }

        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Unauthenticated subscription");
        }

        Long userId = Long.valueOf(accessor.getUser().getName());

        String[] parts = destination.split("/");
        Long conversationId = Long.valueOf(parts[3]);

        boolean activeParticipant =
                participantRepository
                        .existsByConversation_IdAndUser_IdAndLeftAtIsNull(
                                conversationId, userId);

        if (!activeParticipant) {
            throw new IllegalArgumentException(
                    "You are not authorized to subscribe to this conversation");
        }

        return message;
    }
}
