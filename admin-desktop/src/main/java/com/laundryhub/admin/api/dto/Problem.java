package com.laundryhub.admin.api.dto;

/**
 * Problem-details error body.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Problem(String title, String detail, java.util.Map<String, String> errors) {
}
