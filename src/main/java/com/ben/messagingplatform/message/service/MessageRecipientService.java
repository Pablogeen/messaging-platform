package com.ben.messagingplatform.message.service;


import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageRecipientService {

    private final ConversationParticipantRepository participantRepository;

    public List<Long> getRecipientIds(Long conversationId, Long senderId) {

        return participantRepository
                .findByConversation_IdAndLeftAtIsNull(conversationId)
                .stream()
                .map(ConversationParticipant::getUser)
                .map(user -> user.getId())
                .filter(userId -> !userId.equals(senderId))
                .toList();
    }
}

