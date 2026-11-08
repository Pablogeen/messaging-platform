CREATE TABLE outbox_events (
                               id BIGINT NOT NULL AUTO_INCREMENT,

                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id BIGINT NOT NULL,

                               event_type VARCHAR(100) NOT NULL,
                               payload JSON NOT NULL,

                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               retry_count INT NOT NULL DEFAULT 0,

                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               published_at DATETIME(6) NULL,

                               PRIMARY KEY (id),

                               INDEX idx_outbox_status_created (status, created_at),
                               INDEX idx_outbox_aggregate (aggregate_type, aggregate_id),

                               CONSTRAINT chk_outbox_retry_count
                                   CHECK (retry_count >= 0),

                               CONSTRAINT chk_outbox_status
                                   CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);