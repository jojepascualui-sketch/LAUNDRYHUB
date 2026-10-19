package com.laundryhub.admin.api.dto;

/**
 * One order rating.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record RatingItem(Long orderId, String orderNo, String customerName, int stars, String comment, String ratedAt) {
}
