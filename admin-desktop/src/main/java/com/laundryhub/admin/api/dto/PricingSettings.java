package com.laundryhub.admin.api.dto;

/**
 * A3 rates, payment methods and soaps.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record PricingSettings(java.math.BigDecimal ratePerKg, java.math.BigDecimal minKg, java.math.BigDecimal deliveryFee, java.math.BigDecimal deliveryRadiusKm, boolean cashEnabled, boolean gcashEnabled, String shopGcashNumber, java.util.List<Soap> soaps) {
}
