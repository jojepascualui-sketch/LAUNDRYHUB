package com.laundryhub.admin.api.dto;

/**
 * Body of a reply.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record SendMessage(String body) {
}
