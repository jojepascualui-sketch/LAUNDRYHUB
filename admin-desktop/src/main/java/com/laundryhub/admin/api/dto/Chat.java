package com.laundryhub.admin.api.dto;

/**
 * Messages of a conversation, oldest first.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Chat(Long threadId, java.util.List<ChatMessage> messages) {
}
