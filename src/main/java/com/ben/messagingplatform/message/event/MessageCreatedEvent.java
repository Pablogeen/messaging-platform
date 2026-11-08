package com.ben.messagingplatform.message.event;

import java.time.LocalDateTime;

public record MessageCreatedEvent(
        Long messageId,
        Long conversationId,
        Long senderId,
        Long sequenceNumber,
        String content,
        LocalDateTime createdAt
) {
}
