package com.ben.messagingplatform.conversation.repository;

import com.ben.messagingplatform.conversation.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByUser_IdAndLeftAtIsNull(Long userId);

    List<ConversationParticipant> findByConversation_IdAndLeftAtIsNull(Long conversationId);

    boolean existsByConversation_IdAndUser_IdAndLeftAtIsNull(
            Long conversationId, Long userId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_IdAndLeftAtIsNull(
            Long conversationId,
            Long userId
    );
}