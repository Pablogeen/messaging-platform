package com.ben.messagingplatform.message.repository;

import com.ben.messagingplatform.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_IdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            Long conversationId,
            Long sequenceNumber
    );
}
