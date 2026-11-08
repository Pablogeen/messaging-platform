package com.ben.messagingplatform.message.service;


import com.ben.messagingplatform.conversation.entity.Conversation;
import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import com.ben.messagingplatform.conversation.entity.ConversationType;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.conversation.repository.ConversationRepository;
import com.ben.messagingplatform.message.dto.SendMessageRequest;
import com.ben.messagingplatform.message.entity.Message;
import com.ben.messagingplatform.message.repository.MessageRepository;
import com.ben.messagingplatform.message.service.MessageService;
import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.entity.OutboxEventStatus;
import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import com.ben.messagingplatform.user.entity.User;
import com.ben.messagingplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MessageServiceOutboxIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldPersistMessageAndOutboxEventSuccessfully() {
        String uniqueId = UUID.randomUUID().toString();

        User user = userRepository.save(
                new User("testuser_" + uniqueId, uniqueId + "@example.com", "test-password")
        );

        Conversation conversation = conversationRepository.save(
                new Conversation(ConversationType.DIRECT)
        );

        participantRepository.save(
                new ConversationParticipant(conversation, user)
        );

        long initialMessageCount = messageRepository.count();
        long initialOutboxCount = outboxEventRepository.count();
        long initialSequence = conversation.getLastSequence();

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversation.getId());
        request.setContent("Testing successful outbox persistence");

        Message savedMessage = messageService.sendMessage(user.getId(), request);

        assertNotNull(savedMessage.getId());

        assertEquals(initialMessageCount + 1, messageRepository.count());
        assertEquals(initialOutboxCount + 1, outboxEventRepository.count());

        Conversation reloadedConversation = conversationRepository
                .findById(conversation.getId())
                .orElseThrow();

        assertEquals(initialSequence + 1, reloadedConversation.getLastSequence());

        OutboxEvent persistedEvent = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)
                .stream()
                .filter(event ->
                        event.getAggregateType().equals("MESSAGE")
                                && event.getAggregateId().equals(savedMessage.getId())
                                && event.getEventType().equals("MESSAGE_CREATED")
                )
                .findFirst()
                .orElseThrow();

        assertEquals(OutboxEventStatus.PENDING, persistedEvent.getStatus());
    }

}
