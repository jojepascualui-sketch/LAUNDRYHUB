package com.laundryhub.admin.api.dto;

/**
 * A fabric soap (A3 table).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Soap(Long id, String name, String description, java.math.BigDecimal addOnPerKg, boolean included, boolean available, String tintHex, int sortOrder) {
}
