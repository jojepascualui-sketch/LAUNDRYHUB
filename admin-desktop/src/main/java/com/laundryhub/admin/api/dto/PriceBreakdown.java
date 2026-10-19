package com.laundryhub.admin.api.dto;

/**
 * Server-calculated price lines.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record PriceBreakdown(String serviceType, java.math.BigDecimal billableKg, java.math.BigDecimal ratePerKg, java.math.BigDecimal laundryCost, String soapName, java.math.BigDecimal soapCost, java.math.BigDecimal deliveryFee, java.math.BigDecimal total) {
}
