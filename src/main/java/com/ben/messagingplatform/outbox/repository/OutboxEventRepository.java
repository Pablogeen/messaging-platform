package com.ben.messagingplatform.outbox.repository;

import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    @Query(value = """
    SELECT * FROM outbox_events
    WHERE status = :status
    ORDER BY created_at ASC
    LIMIT :batchSize
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxEvent> claimPendingEvents(
            @Param("status") String status,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE outbox_events
        SET status = CASE
                WHEN retry_count + 1 >= :maxRetries THEN 'FAILED'
                ELSE 'PENDING'
            END,
            retry_count = retry_count + 1,
            processing_at = NULL
        WHERE status = 'PROCESSING'
          AND processing_at < :cutoff
        """, nativeQuery = true)
    int recoverStaleProcessingEvents(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("maxRetries") int maxRetries
    );
}
