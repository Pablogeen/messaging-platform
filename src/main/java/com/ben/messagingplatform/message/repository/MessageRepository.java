package com.ben.messagingplatform.message.repository;

import com.ben.messagingplatform.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {



    Slice<Message> findByConversation_IdAndSequenceNumberGreaterThanAndCreatedAtGreaterThanEqualOrderBySequenceNumberAsc(
            Long conversationId,
            Long sequenceNumber,
            LocalDateTime joinedAt,
            Pageable pageable
    );

    boolean existsByConversation_IdAndSequenceNumber(
            Long conversationId,
            Long sequenceNumber
    );

    Optional<Message> findByConversation_IdAndSequenceNumber(
            Long conversationId,
            Long sequenceNumber
    );
}
