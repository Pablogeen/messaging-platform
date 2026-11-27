package com.ben.messagingplatform.message.service;

import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.message.dto.MessageHistoryResponse;
import com.ben.messagingplatform.message.dto.MessageResponse;
import com.ben.messagingplatform.message.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    private static final int MAX_BATCH_SIZE = 100;

    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(
            Long conversationId,
            Long userId,
            Long afterSequence,
            int size
    ) {
        if (afterSequence < 0) {
            throw new IllegalArgumentException("Sequence cursor cannot be negative");
        }

        if (size < 1 || size > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size must be between 1 and 100");
        }

        ConversationParticipant participant = participantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active conversation membership not found"));

        var slice = messageRepository
                .findByConversation_IdAndSequenceNumberGreaterThanAndCreatedAtGreaterThanEqualOrderBySequenceNumberAsc(
                        conversationId,
                        afterSequence,
                        participant.getJoinedAt(),
                        PageRequest.of(0, size)
                );

        var messages = slice.getContent().stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getConversation().getId(),
                        message.getSender().getId(),
                        message.getSequenceNumber(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();

        Long nextCursor = messages.isEmpty()
                ? afterSequence
                : messages.getLast().sequenceNumber();

        return new MessageHistoryResponse(
                messages,
                nextCursor,
                slice.hasNext()
        );
    }
}