package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;

/**
 * A3 Pricing &amp; soaps form.
 *
 * @param ratePerKg        "Price per kilo (₱)"
 * @param minKg            "Minimum load (kg)"
 * @param deliveryFee      "Pickup &amp; delivery fee (₱)"
 * @param deliveryRadiusKm "Delivery radius (km)"
 * @param cashEnabled      Cash checkbox
 * @param gcashEnabled     GCash checkbox
 * @param shopGcashNumber  "Shop GCash number"
 * @param updatedAt        last saved
 * @param soaps            "Fabric soaps" table, including unavailable ones
 */
public record PricingSettingsResponse(BigDecimal ratePerKg, BigDecimal minKg, BigDecimal deliveryFee,
                                      BigDecimal deliveryRadiusKm, boolean cashEnabled, boolean gcashEnabled,
                                      String shopGcashNumber, LocalDateTime updatedAt, List<SoapResponse> soaps) {

    /**
     * @param settings pricing row
     * @param soaps    all soaps
     * @return the DTO
     */
    public static PricingSettingsResponse from(PricingSettings settings, List<SoapResponse> soaps) {
        return new PricingSettingsResponse(settings.getRatePerKg(), settings.getMinKg(), settings.getDeliveryFee(),
                settings.getDeliveryRadiusKm(), settings.isCashEnabled(), settings.isGcashEnabled(),
                settings.getShopGcashNumber(), settings.getUpdatedAt(), soaps);
    }
}
