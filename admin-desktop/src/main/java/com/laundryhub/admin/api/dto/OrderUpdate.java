package com.laundryhub.admin.api.dto;

/**
 * Body of {@code PATCH /api/admin/orders/{id}}: a new status or a rider (nulls are left out).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record OrderUpdate(String status, Long riderId) {
}
