CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(100) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE conversations (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               type VARCHAR(20) NOT NULL,
                               last_sequence BIGINT NOT NULL DEFAULT 0,
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT pk_conversations PRIMARY KEY (id),
                               CONSTRAINT chk_conversation_type
                                   CHECK (type IN ('DIRECT', 'GROUP'))
);

CREATE TABLE conversation_participants (
                                           conversation_id BIGINT NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                           left_at DATETIME(6) NULL,
                                           last_read_sequence BIGINT NOT NULL DEFAULT 0,

                                           CONSTRAINT pk_conversation_participants
                                               PRIMARY KEY (conversation_id, user_id),

                                           CONSTRAINT fk_participants_conversation
                                               FOREIGN KEY (conversation_id)
                                                   REFERENCES conversations(id),

                                           CONSTRAINT fk_participants_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES users(id)
);

CREATE TABLE messages (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          conversation_id BIGINT NOT NULL,
                          sender_id BIGINT NOT NULL,
                          sequence_number BIGINT NOT NULL,
                          content TEXT NOT NULL,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                          CONSTRAINT pk_messages PRIMARY KEY (id),

                          CONSTRAINT fk_messages_conversation
                              FOREIGN KEY (conversation_id)
                                  REFERENCES conversations(id),

                          CONSTRAINT fk_messages_sender
                              FOREIGN KEY (sender_id)
                                  REFERENCES users(id),

                          CONSTRAINT uk_conversation_sequence
                              UNIQUE (conversation_id, sequence_number)
);

CREATE INDEX idx_participants_user
    ON conversation_participants(user_id);

CREATE INDEX idx_messages_sender_created
    ON messages(sender_id, created_at);