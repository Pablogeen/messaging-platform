package com.ben.messagingplatform.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "Conversation is required")
    private Long conversationId;

    @NotBlank(message = "Content is required")
    private String content;
}
