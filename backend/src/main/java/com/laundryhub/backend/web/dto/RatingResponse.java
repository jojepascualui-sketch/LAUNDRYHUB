package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.feedback.OrderRating;

import java.time.LocalDateTime;

/**
 * A rating of a completed order.
 *
 * @param orderId      order id
 * @param orderNo      "LH-0001"
 * @param customerName who rated
 * @param stars        1–5
 * @param comment      optional comment
 * @param ratedAt      last change
 */
public record RatingResponse(Long orderId, String orderNo, String customerName, int stars, String comment,
                             LocalDateTime ratedAt) {

    /**
     * @param rating the rating
     * @return the response
     */
    public static RatingResponse from(OrderRating rating) {
        return new RatingResponse(rating.getOrder().getId(), rating.getOrder().getOrderNo(),
                rating.getCustomer().getFullName(), rating.getStars(), rating.getComment(), rating.getUpdatedAt());
    }
}
