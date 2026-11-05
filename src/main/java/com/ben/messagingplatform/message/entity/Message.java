package com.ben.messagingplatform.message.entity;

import com.ben.messagingplatform.conversation.entity.Conversation;
import com.ben.messagingplatform.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_sequence",
                        columnNames = {"conversation_id", "sequence_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_messages_sender_created",
                        columnList = "sender_id, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Message(Conversation conversation, User sender, Long sequenceNumber, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.sequenceNumber = sequenceNumber;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
