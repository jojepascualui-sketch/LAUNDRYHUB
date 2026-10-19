package com.laundryhub.admin.api.dto;

/**
 * A conversation in the Feedback list.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record ChatThread(Long id, Long userId, String userName, String initials, String role, String lastMessage, String lastMessageAt, int unread) {
}
