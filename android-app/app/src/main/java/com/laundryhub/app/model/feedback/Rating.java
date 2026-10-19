package com.laundryhub.app.model.feedback;

import androidx.annotation.Nullable;

/**
 * A 1–5 star rating of a completed order.
 *
 * @param stars   1–5
 * @param comment optional comment
 */
public record Rating(int stars, @Nullable String comment) {

    /** Lowest rating. */
    public static final int MIN_STARS = 1;
    /** Highest rating. */
    public static final int MAX_STARS = 5;
    /** Longest comment (server limit). */
    public static final int MAX_COMMENT = 500;

    /** Validates the stars and comment. */
    public Rating {
        if (stars < MIN_STARS || stars > MAX_STARS) {
            throw new IllegalArgumentException("Stars must be between " + MIN_STARS + " and " + MAX_STARS);
        }
        comment = comment == null || comment.isBlank() ? null : comment.trim();
        if (comment != null && comment.length() > MAX_COMMENT) {
            throw new IllegalArgumentException("Comment is too long");
        }
    }
}
