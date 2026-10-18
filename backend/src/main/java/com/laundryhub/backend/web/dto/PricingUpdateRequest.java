package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A3 "Save changes" (rates and payment methods; soaps have their own endpoints).
 *
 * @param ratePerKg        price per kilo
 * @param minKg            minimum load
 * @param deliveryFee      pickup &amp; delivery fee
 * @param deliveryRadiusKm delivery radius
 * @param cashEnabled      Cash offered
 * @param gcashEnabled     GCash offered
 * @param shopGcashNumber  required when GCash is on
 */
public record PricingUpdateRequest(
        @NotNull @Positive BigDecimal ratePerKg,
        @NotNull @Positive BigDecimal minKg,
        @NotNull @PositiveOrZero BigDecimal deliveryFee,
        @NotNull @Positive BigDecimal deliveryRadiusKm,
        @NotNull Boolean cashEnabled,
        @NotNull Boolean gcashEnabled,
        @Pattern(regexp = ApiValidation.MOBILE_REGEX, message = ApiValidation.MOBILE_MESSAGE) String shopGcashNumber) {
}
