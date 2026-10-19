package com.laundryhub.admin.api.dto;

/**
 * A rider job of a delivery order.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Job(Long id, String type, String typeLabel, String status, Long riderId, String riderName) {
}
