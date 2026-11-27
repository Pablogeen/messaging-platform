ALTER TABLE conversation_participants
    ADD COLUMN last_delivered_sequence BIGINT NOT NULL DEFAULT 0;