package com.laundryhub.app.model.feedback;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

/**
 * One chat bubble.
 *
 * @param id         message id (increasing)
 * @param mine       written by this user (right, accent) or by the shop (left, white)
 * @param senderName who wrote it
 * @param body       the text
 * @param sentAt     when
 */
public record ChatMessage(long id, boolean mine, @NonNull String senderName, @NonNull String body,
                          @NonNull LocalDateTime sentAt) {

    /** Validates the text. */
    public ChatMessage {
        if (body == null || body.isBlank() || sentAt == null) {
            throw new IllegalArgumentException("Message text and time are required");
        }
        if (senderName == null) {
            senderName = "";
        }
    }
}
