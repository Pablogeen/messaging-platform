package com.ben.messagingplatform.message.service;

import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import com.ben.messagingplatform.conversation.repository.ConversationParticipantRepository;
import com.ben.messagingplatform.message.dto.MessageAcknowledgementRequest;
import com.ben.messagingplatform.message.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageAcknowledgementService {

    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void acknowledgeDelivery(Long userId, MessageAcknowledgementRequest request) {
        var participant = participantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(
                        request.conversationId(), userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active conversation membership not found"));

        validateSequence(request, participant);

        participant.acknowledgeDelivery(request.sequenceNumber());
    }

    @Transactional
    public void acknowledgeRead(Long userId, MessageAcknowledgementRequest request) {
        var participant = participantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(
                        request.conversationId(), userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active conversation membership not found"));

        validateSequence(request, participant);

        // Reading a message also implies that it was delivered.
        participant.acknowledgeDelivery(request.sequenceNumber());
        participant.acknowledgeRead(request.sequenceNumber());
    }

    private void validateSequence(MessageAcknowledgementRequest request, ConversationParticipant participant) {
        Long sequence = request.sequenceNumber();

        if (sequence == null || sequence < 0) {
            throw new IllegalArgumentException("Invalid message sequence");
        }

        if (sequence == 0) {
            return;
        }

        var message = messageRepository
                .findByConversation_IdAndSequenceNumber(
                        request.conversationId(), sequence)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Message does not exist in this conversation"));

        if (message.getCreatedAt().isBefore(participant.getJoinedAt())) {
            throw new IllegalArgumentException(
                    "Cannot acknowledge a message sent before joining the conversation");
        }
    }
}
