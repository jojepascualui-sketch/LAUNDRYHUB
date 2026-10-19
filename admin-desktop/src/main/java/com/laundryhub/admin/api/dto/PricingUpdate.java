package com.laundryhub.admin.api.dto;

/**
 * Body of {@code PUT /api/admin/pricing}.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record PricingUpdate(java.math.BigDecimal ratePerKg, java.math.BigDecimal minKg, java.math.BigDecimal deliveryFee, java.math.BigDecimal deliveryRadiusKm, Boolean cashEnabled, Boolean gcashEnabled, String shopGcashNumber) {
}
