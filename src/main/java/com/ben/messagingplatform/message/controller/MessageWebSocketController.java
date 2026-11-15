package com.ben.messagingplatform.message.controller;

import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.message.dto.MessageResponse;
import com.ben.messagingplatform.message.dto.SendMessageRequest;
import com.ben.messagingplatform.message.entity.Message;
import com.ben.messagingplatform.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;
    private final ConversationParticipantRepository participantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/messages.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        Message message = messageService.sendMessage(senderId, request);

        Long conversationId = message.getConversation().getId();

        MessageResponse response = new MessageResponse(
                message.getId(),
                conversationId,
                message.getSender().getId(),
                message.getSequenceNumber(),
                message.getContent(),
                message.getCreatedAt()
        );

        var participants = participantRepository
                .findByConversation_IdAndLeftAtIsNull(conversationId);

        for (var participant : participants) {
            messagingTemplate.convertAndSendToUser(
                    participant.getUser().getId().toString(),
                    "/queue/messages",
                    response
            );
        }
    }
}