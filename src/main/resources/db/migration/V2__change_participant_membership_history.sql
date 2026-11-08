ALTER TABLE conversation_participants
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD UNIQUE KEY uk_conversation_participants_id (id),
    ADD KEY idx_participant_conversation (conversation_id),
    ADD KEY idx_participant_user_conversation (user_id, conversation_id),
DROP PRIMARY KEY,
DROP INDEX idx_participants_user,
    ADD PRIMARY KEY (id);