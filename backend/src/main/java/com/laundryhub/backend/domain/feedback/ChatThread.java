package com.laundryhub.backend.domain.feedback;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.user.User;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The one feedback conversation between a customer or rider and the shop. It keeps an unread counter for
 * each side so the app and the console can show new replies.
 */
@Entity
@Table(name = "chat_threads")
// OOP: Inheritance — an AuditedEntity (id and timestamps).
public class ChatThread extends AuditedEntity {

    // OOP: Encapsulation — the owner is fixed; counters change only through post() and markReadBy().
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "shop_unread", nullable = false)
    private int shopUnread;

    @Column(name = "user_unread", nullable = false)
    private int userUnread;

    /** For JPA only. */
    protected ChatThread() {
    }

    /**
     * @param user the customer or rider
     * @throws IllegalArgumentException if the user is missing or a console account (admin/staff answer threads)
     */
    public ChatThread(User user) {
        this.user = Check.notNull(user, "User");
        if (user.chatSide() != MessageSide.USER) {
            throw new IllegalArgumentException("Admin and staff answer conversations; they do not own one");
        }
    }

    /**
     * Writes a message. The side comes from the sender: the owner writes as USER, admin/staff as SHOP.
     *
     * @param sender who writes
     * @param body   text, 1–{@value ChatMessage#MAX_BODY} characters
     * @return the new message (to be saved)
     * @throws IllegalArgumentException if the sender is neither the owner nor a console user, or the text is invalid
     */
    public ChatMessage post(User sender, String body) {
        Check.notNull(sender, "Sender");
        // OOP: Polymorphism — User.chatSide() answers USER for customers/riders and SHOP for console users.
        MessageSide side = sender.chatSide();
        if (side == MessageSide.USER && !sender.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the owner or the shop can write in this conversation");
        }
        ChatMessage message = new ChatMessage(this, sender, side, body);
        lastMessageAt = message.getSentAt();
        if (side == MessageSide.USER) {
            shopUnread++;
        } else {
            userUnread++;
        }
        return message;
    }

    /**
     * @param reader the side that opened the conversation
     */
    public void markReadBy(MessageSide reader) {
        if (reader == MessageSide.SHOP) {
            shopUnread = 0;
        } else {
            userUnread = 0;
        }
    }

    /** @return the customer or rider */
    public User getUser() {
        return user;
    }

    /** @return when the last message was sent, or {@code null} */
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    /** @return messages the shop has not read */
    public int getShopUnread() {
        return shopUnread;
    }

    /** @return replies the user has not read */
    public int getUserUnread() {
        return userUnread;
    }
}
