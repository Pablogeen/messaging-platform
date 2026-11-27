package com.ben.messagingplatform.message.controller;

import com.ben.messagingplatform.message.dto.MessageHistoryResponse;
import com.ben.messagingplatform.message.service.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class MessageHistoryController {

    private final MessageHistoryService messageHistoryService;

    @GetMapping("/{conversationId}/messages")
    public MessageHistoryResponse getMessageHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") Long afterSequence,
            @RequestParam(defaultValue = "50") int size,
            Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());

        return messageHistoryService.getHistory(
                conversationId,
                userId,
                afterSequence,
                size
        );
    }
}