package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

/**
 * The big button on R2/R3. The body is optional.
 *
 * @param weighedKg kilos on the scale, only read on the pickup step "Laundry picked up &amp; weighed"
 *                  ("Confirm pickup"); re-prices the order (below the minimum is charged as the minimum)
 */
public record AdvanceJobRequest(@Positive BigDecimal weighedKg) {
}
