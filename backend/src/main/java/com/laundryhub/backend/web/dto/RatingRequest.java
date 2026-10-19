package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/orders/{id}/rating}.
 *
 * @param stars   1–5
 * @param comment optional, up to 500 characters
 */
public record RatingRequest(@Min(1) @Max(5) int stars, @Size(max = 500) String comment) {
}
