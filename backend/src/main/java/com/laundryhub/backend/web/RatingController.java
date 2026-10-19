package com.laundryhub.backend.web;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.FeedbackService;
import com.laundryhub.backend.web.dto.RatingRequest;
import com.laundryhub.backend.web.dto.RatingResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Customer ratings of completed orders (07 Track order, no design yet). */
@RestController
@RequestMapping("/api/orders/{id}/rating")
@Tag(name = "Feedback")
public class RatingController {

    private final FeedbackService feedback;

    /**
     * @param feedback rating logic
     */
    public RatingController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    /**
     * @param customerId signed-in customer
     * @param id         their order
     * @return the rating, or 204 when not rated yet
     */
    @GetMapping
    @Operation(summary = "My rating of this order (204 if none)")
    public ResponseEntity<RatingResponse> get(@CurrentUserId Long customerId, @PathVariable Long id) {
        return feedback.rating(customerId, id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * @param customerId signed-in customer
     * @param id         their completed order
     * @param request    stars and comment
     * @return the saved rating; 409 if the order is not completed
     */
    @PutMapping
    @Operation(summary = "Rate a completed order 1–5 stars (or change the rating)")
    public RatingResponse rate(@CurrentUserId Long customerId, @PathVariable Long id,
                               @Valid @RequestBody RatingRequest request) {
        return feedback.rate(customerId, id, request.stars(), request.comment());
    }
}
