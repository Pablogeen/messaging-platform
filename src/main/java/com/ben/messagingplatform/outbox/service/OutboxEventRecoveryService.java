package com.ben.messagingplatform.outbox.service;

import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxEventRecoveryService {

    private static final int MAX_RETRIES = 3;
    private static final int PROCESSING_TIMEOUT_MINUTES = 15;

    private final OutboxEventRepository outboxEventRepository;

    public int recoverStaleEvents() {

        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(PROCESSING_TIMEOUT_MINUTES);

        return outboxEventRepository.recoverStaleProcessingEvents(
                cutoff,
                MAX_RETRIES
        );
    }
}
