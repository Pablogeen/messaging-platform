ALTER TABLE outbox_events
    ADD COLUMN processing_at DATETIME(6) NULL;