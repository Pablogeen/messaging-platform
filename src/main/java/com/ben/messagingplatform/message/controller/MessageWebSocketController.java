
package com.ben.messagingplatform.message.controller;

import com.ben.messagingplatform.message.dto.SendMessageRequest;
import com.ben.messagingplatform.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;

    @MessageMapping("/messages.send")
    public void sendMessage(
            SendMessageRequest request,
            Principal principal
    ) {

        Long senderId = Long.valueOf(principal.getName());

        messageService.sendMessage(senderId, request);
    }
}
