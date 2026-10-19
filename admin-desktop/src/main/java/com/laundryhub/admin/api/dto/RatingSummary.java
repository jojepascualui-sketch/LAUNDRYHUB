package com.laundryhub.admin.api.dto;

/**
 * Ratings card: average, count and the latest ratings.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record RatingSummary(java.math.BigDecimal average, long count, java.util.List<RatingItem> latest) {
}
