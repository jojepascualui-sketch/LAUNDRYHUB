package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A GPS fix from the rider's phone.
 *
 * @param lat            latitude
 * @param lng            longitude
 * @param accuracyMeters GPS accuracy, optional
 */
public record RiderLocationRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
        @PositiveOrZero BigDecimal accuracyMeters) {
}
