package com.laundryhub.backend.domain.feedback;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.user.User;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One message of a feedback conversation. Immutable once written. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    /** Longest message, in characters. */
    public static final int MAX_BODY = 1000;
    private static final int MAX_NAME = 120;

    // OOP: Encapsulation — set once in the constructor, no setters.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false, updatable = false)
    private ChatThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", updatable = false)
    private User sender;

    @Column(name = "sender_name", nullable = false, length = MAX_NAME, updatable = false)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_side", nullable = false, length = 10, updatable = false)
    private MessageSide side;

    @Column(name = "body", nullable = false, length = MAX_BODY, updatable = false)
    private String body;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    /** For JPA only. */
    protected ChatMessage() {
    }

    ChatMessage(ChatThread thread, User sender, MessageSide side, String body) {
        this.thread = Check.notNull(thread, "Conversation");
        this.sender = Check.notNull(sender, "Sender");
        this.side = Check.notNull(side, "Side");
        this.senderName = sender.getFullName();
        this.body = Check.text(body, "Message", MAX_BODY);
        this.sentAt = LocalDateTime.now();
    }

    /** @return message id (increasing; clients poll with "after") */
    public Long getId() {
        return id;
    }

    /** @return the conversation */
    public ChatThread getThread() {
        return thread;
    }

    /** @return the sender's name when the message was written */
    public String getSenderName() {
        return senderName;
    }

    /** @return USER or SHOP */
    public MessageSide getSide() {
        return side;
    }

    /** @return the text */
    public String getBody() {
        return body;
    }

    /** @return when it was sent */
    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
