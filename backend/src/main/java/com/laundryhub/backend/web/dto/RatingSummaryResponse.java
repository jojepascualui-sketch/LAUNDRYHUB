package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ratings card of the console's Feedback page.
 *
 * @param average average stars rounded to 1 decimal, or {@code null} with no ratings
 * @param count   number of rated orders
 * @param latest  newest ratings first
 */
public record RatingSummaryResponse(BigDecimal average, long count, List<RatingResponse> latest) {
}
