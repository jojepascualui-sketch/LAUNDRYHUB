package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.Role;

import java.time.LocalDateTime;

/**
 * A conversation in the console's Feedback list.
 *
 * @param id            conversation id
 * @param userId        customer or rider id
 * @param userName      their name
 * @param initials      avatar initials
 * @param role          CUSTOMER or RIDER
 * @param lastMessage   newest message text, shortened
 * @param lastMessageAt when it was sent
 * @param unread        messages the shop has not opened
 */
public record ChatThreadResponse(Long id, Long userId, String userName, String initials, Role role, String lastMessage,
                                 LocalDateTime lastMessageAt, int unread) {
}
