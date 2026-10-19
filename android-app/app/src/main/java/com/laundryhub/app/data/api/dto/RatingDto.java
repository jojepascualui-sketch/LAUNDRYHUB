package com.laundryhub.app.data.api.dto;

/** Request and response of {@code /api/orders/{id}/rating}. Filled by Gson. */
public final class RatingDto {

    // OOP: Encapsulation — set once.
    private int stars;
    private String comment;

    /** For Gson. */
    public RatingDto() {
    }

    /**
     * @param stars   1–5
     * @param comment optional comment
     */
    public RatingDto(int stars, String comment) {
        this.stars = stars;
        this.comment = comment;
    }

    /** @return 1–5 */
    public int getStars() {
        return stars;
    }

    /** @return the comment, or {@code null} */
    public String getComment() {
        return comment;
    }
}
