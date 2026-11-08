package com.ben.messagingplatform.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}