package com.ben.messagingplatform.message.dto;


public record MessageReceiptEvent(
        Long conversationId,
        Long userId,
        Long sequenceNumber,
        ReceiptType type
) {
    public enum ReceiptType {
        DELIVERED,
        READ
    }
}
