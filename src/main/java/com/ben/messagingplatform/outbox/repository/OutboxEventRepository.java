package com.ben.messagingplatform.outbox.repository;

import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
