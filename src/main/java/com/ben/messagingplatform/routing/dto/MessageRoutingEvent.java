package com.ben.messagingplatform.routing.dto;

import java.time.LocalDateTime;

public record MessageRoutingEvent(
        String eventId,
        Long recipientId,
        Long conversationId,
        Long messageId,
        Long senderId,
        String content,
        Long sequenceNumber,
        LocalDateTime createdAt,
        String targetServerId
) {
}

