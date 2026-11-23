package com.ben.messagingplatform.message.dto;

import java.util.List;

public record MessageHistoryResponse(
        List<MessageResponse> messages,
        Long nextCursor,
        boolean hasMore
) {}
