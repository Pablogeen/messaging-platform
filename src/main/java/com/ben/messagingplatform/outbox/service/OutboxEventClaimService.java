package com.ben.messagingplatform.outbox.service;


import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.entity.OutboxEventStatus;
import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.claimPendingEvents(
                        OutboxEventStatus.PENDING.name(),
                        BATCH_SIZE
                );

        for (OutboxEvent event : events) {
            event.markProcessing();
        }

        outboxEventRepository.saveAll(events);

        return events;
    }
}