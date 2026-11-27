package com.ben.messagingplatform.message.dto;

public record MessageAcknowledgementRequest(
        Long conversationId,
        Long sequenceNumber
) {}
