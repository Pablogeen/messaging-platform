package com.ben.messagingplatform.message.service;

import com.ben.messagingplatform.conversation.entity.Conversation;
import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import com.ben.messagingplatform.conversation.entity.ConversationType;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.conversation.repository.ConversationRepository;
import com.ben.messagingplatform.message.dto.SendMessageRequest;
import com.ben.messagingplatform.message.event.MessageCreatedEvent;
import com.ben.messagingplatform.message.repository.MessageRepository;
import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import com.ben.messagingplatform.outbox.service.OutboxEventService;
import com.ben.messagingplatform.user.entity.User;
import com.ben.messagingplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @MockitoBean
    private OutboxEventService outboxService;

    @Test
    void shouldRollbackMessageAndSequenceWhenOutboxSaveFails() {

        // 1. Create test data
        String uniqueId = UUID.randomUUID().toString();

        User user = userRepository.save(
                new User(
                        "testuser_" + uniqueId,
                        uniqueId + "@example.com",
                        "test-password"
                )
        );

        Conversation conversation = conversationRepository.save(
                new Conversation(ConversationType.DIRECT)
        );

        participantRepository.save(
                new ConversationParticipant(conversation, user)
        );

        // 2. Capture initial database state
        long initialMessageCount = messageRepository.count();
        long initialOutboxCount = outboxEventRepository.count();
        long initialSequence = conversation.getLastSequence();

        // 3. Configure the outbox failure
        doThrow(new IllegalStateException("Simulated outbox failure"))
                .when(outboxService)
                .saveEvent(anyString(), anyLong(), anyString(), any());

        // 4. Prepare the message request
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversation.getId());
        request.setContent("Testing transactional rollback");

        // 5. Verify that sending the message throws an exception
        assertThrows(
                IllegalStateException.class,
                () -> messageService.sendMessage(user.getId(), request)
        );

        verify(outboxService).saveEvent(
                eq("MESSAGE"),
                anyLong(),
                eq("MESSAGE_CREATED"),
                any(MessageCreatedEvent.class)
        );

        // 6. Verify that no message or outbox event was persisted
        assertEquals(initialMessageCount, messageRepository.count());
        assertEquals(initialOutboxCount, outboxEventRepository.count());

        // 7. Reload the conversation and verify its sequence was rolled back
        Conversation reloadedConversation = conversationRepository
                .findById(conversation.getId())
                .orElseThrow();

        assertEquals(initialSequence, reloadedConversation.getLastSequence());
    }
}