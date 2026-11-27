package com.ben.messagingplatform.message.controller;

import com.ben.messagingplatform.message.dto.MessageAcknowledgementRequest;
import com.ben.messagingplatform.message.dto.MessageReceiptEvent;
import com.ben.messagingplatform.message.service.MessageAcknowledgementService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageAcknowledgementController {

    private final MessageAcknowledgementService acknowledgementService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/acknowledge-delivery")
    public void acknowledgeDelivery(@RequestBody MessageAcknowledgementRequest request, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        acknowledgementService.acknowledgeDelivery(userId, request);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.conversationId() + "/receipts",
                new MessageReceiptEvent(
                        request.conversationId(),
                        userId,
                        request.sequenceNumber(),
                        MessageReceiptEvent.ReceiptType.DELIVERED
                )
        );
    }

    @PostMapping("/acknowledge-read")
    public void acknowledgeRead(@RequestBody MessageAcknowledgementRequest request, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        acknowledgementService.acknowledgeRead(userId, request);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.conversationId() + "/receipts",
                new MessageReceiptEvent(
                        request.conversationId(),
                        userId,
                        request.sequenceNumber(),
                        MessageReceiptEvent.ReceiptType.READ
                )
        );
    }
}
