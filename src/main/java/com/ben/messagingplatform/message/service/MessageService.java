package com.ben.messagingplatform.message.service;

import com.ben.messagingplatform.conversation.entity.Conversation;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.conversation.repository.ConversationRepository;
import com.ben.messagingplatform.message.dto.SendMessageRequest;
import com.ben.messagingplatform.message.entity.Message;
import com.ben.messagingplatform.message.event.MessageCreatedEvent;
import com.ben.messagingplatform.message.exception.InactiveConversationMemberException;
import com.ben.messagingplatform.message.repository.MessageRepository;
import com.ben.messagingplatform.outbox.service.OutboxEventService;
import com.ben.messagingplatform.user.entity.User;
import com.ben.messagingplatform.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final OutboxEventService outboxService;
    private final ObjectMapper objectMapper;


    @Transactional
    public Message sendMessage(Long senderId, SendMessageRequest request) {

        Conversation conversation = conversationRepository
                .findByIdWithLock(request.getConversationId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Conversation not found"));

        boolean isParticipant = participantRepository
                .existsByConversation_IdAndUser_IdAndLeftAtIsNull(
                        conversation.getId(),
                        senderId);

        if (!isParticipant) {
            throw new InactiveConversationMemberException("You are not an active participant in this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Sender not found"));

        long nextSequence = conversation.getLastSequence() + 1;

        conversation.setLastSequence(nextSequence);

        Message message = new Message(
                conversation,
                sender,
                nextSequence,
                request.getContent());


        Message savedMessage = messageRepository.save(message);

        MessageCreatedEvent event = new MessageCreatedEvent(savedMessage.getId(), conversation.getId(), sender.getId(),
                savedMessage.getSequenceNumber(), savedMessage.getContent(), savedMessage.getCreatedAt());

        outboxService.saveEvent("MESSAGE", savedMessage.getId(), "MESSAGE_CREATED", event);

        return savedMessage;
    }

}
