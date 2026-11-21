package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;


import com.ben.messagingplatform.outbox.service.OutboxEventRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRecoveryScheduler {

    private final OutboxEventRecoveryService recoveryService;

    @Scheduled(fixedDelay = 60000)
    public void recoverStaleEvents() {

        int recoveredCount = recoveryService.recoverStaleEvents();

        if (recoveredCount > 0) {
            log.info("Recovered {} stale outbox events", recoveredCount);
        }
    }
}
