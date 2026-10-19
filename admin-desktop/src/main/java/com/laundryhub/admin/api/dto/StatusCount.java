package com.laundryhub.admin.api.dto;

/**
 * Orders per status (A1 pipeline, A2 filter chips).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record StatusCount(String status, String label, long count) {
}
