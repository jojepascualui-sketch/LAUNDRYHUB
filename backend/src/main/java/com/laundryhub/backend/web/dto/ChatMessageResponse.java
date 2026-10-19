package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.feedback.ChatMessage;
import com.laundryhub.backend.domain.feedback.MessageSide;

import java.time.LocalDateTime;

/**
 * One chat bubble.
 *
 * @param id         message id (poll with {@code after=<last id>})
 * @param side       USER (customer/rider) or SHOP (admin/staff)
 * @param senderName who wrote it
 * @param mine       whether the viewer's side wrote it (right-aligned bubble)
 * @param body       text
 * @param sentAt     when
 */
public record ChatMessageResponse(Long id, MessageSide side, String senderName, boolean mine, String body,
                                  LocalDateTime sentAt) {

    /**
     * @param message the message
     * @param viewer  the side looking at it
     * @return the bubble for that viewer
     */
    public static ChatMessageResponse from(ChatMessage message, MessageSide viewer) {
        return new ChatMessageResponse(message.getId(), message.getSide(), message.getSenderName(),
                message.getSide() == viewer, message.getBody(), message.getSentAt());
    }
}
