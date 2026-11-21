package com.ben.messagingplatform.outbox.service;


import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.entity.OutboxEventStatus;
import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxEventRecoveryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventRecoveryService recoveryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRecoverStaleProcessingEvent() {

        OutboxEvent event = new OutboxEvent(
                "MESSAGE",
                100L,
                "MESSAGE_CREATED",
                "{\"messageId\":100}"
        );

        event.markProcessing();

        event = outboxEventRepository.save(event);

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET processing_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)),
                event.getId()
        );

        int recoveredCount = recoveryService.recoverStaleEvents();

        OutboxEvent recoveredEvent = outboxEventRepository
                .findById(event.getId())
                .orElseThrow();

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(recoveredEvent.getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);
        assertThat(recoveredEvent.getRetryCount()).isEqualTo(1);
        assertThat(recoveredEvent.getProcessingAt()).isNull();
    }

    @Test
    void shouldMarkEventAsFailedWhenMaximumRetriesReached() {

        OutboxEvent event = new OutboxEvent(
                "MESSAGE",
                101L,
                "MESSAGE_CREATED",
                "{\"messageId\":101}"
        );

        event.markProcessing();
        event = outboxEventRepository.save(event);

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET retry_count = 2,
                    processing_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)),
                event.getId()
        );

        recoveryService.recoverStaleEvents();

        OutboxEvent recoveredEvent = outboxEventRepository
                .findById(event.getId())
                .orElseThrow();

        assertThat(recoveredEvent.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(recoveredEvent.getRetryCount()).isEqualTo(3);

        assertThat(recoveredEvent.getProcessingAt()).isNull();
    }
}
