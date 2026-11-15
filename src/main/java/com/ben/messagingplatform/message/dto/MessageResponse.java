package com.ben.messagingplatform.message.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        Long sequenceNumber,
        String content,
        LocalDateTime createdAt
) {}
