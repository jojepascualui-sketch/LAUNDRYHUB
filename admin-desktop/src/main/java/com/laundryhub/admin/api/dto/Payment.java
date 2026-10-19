package com.laundryhub.admin.api.dto;

/**
 * Payment method and status.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Payment(String method, String displayName, String status) {
}
