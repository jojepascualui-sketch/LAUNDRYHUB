package com.laundryhub.admin.api.dto;

/**
 * Body to create or change a soap.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record SoapRequest(String name, String description, java.math.BigDecimal addOnPerKg, Boolean available, String tintHex, Integer sortOrder) {
}
