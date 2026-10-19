package com.laundryhub.admin.api.dto;

/**
 * Pickup / drop-off slot.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Schedule(String text) {
}
