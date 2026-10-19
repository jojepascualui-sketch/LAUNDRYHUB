package com.laundryhub.admin.api.dto;

/**
 * One feedback chat bubble.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record ChatMessage(Long id, String side, String senderName, boolean mine, String body, String sentAt) {
}
