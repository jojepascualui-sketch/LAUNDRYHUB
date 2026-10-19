package com.laundryhub.backend.web.admin;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.FeedbackService;
import com.laundryhub.backend.web.dto.ChatMessageResponse;
import com.laundryhub.backend.web.dto.ChatResponse;
import com.laundryhub.backend.web.dto.ChatThreadResponse;
import com.laundryhub.backend.web.dto.RatingSummaryResponse;
import com.laundryhub.backend.web.dto.SendMessageRequest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Console Feedback page: conversations, replies and ratings (admin and staff). */
@RestController
@RequestMapping("/api/admin/feedback")
@Tag(name = "Admin feedback", description = "Console Feedback page (no design yet)")
public class AdminFeedbackController {

    private final FeedbackService feedback;

    /**
     * @param feedback chat and rating logic
     */
    public AdminFeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    /**
     * @param editorId signed-in admin or staff member
     * @return conversations, newest activity first
     */
    @GetMapping("/threads")
    @Operation(summary = "Conversations with customers and riders")
    public List<ChatThreadResponse> threads(@CurrentUserId Long editorId) {
        return feedback.threads(editorId);
    }

    /**
     * @param editorId signed-in admin or staff member
     * @param id       conversation
     * @param after    last message id the console has, or none
     * @return the messages (marks them read by the shop)
     */
    @GetMapping("/threads/{id}/messages")
    @Operation(summary = "Messages of a conversation (poll with after=<last id>)")
    public ChatResponse messages(@CurrentUserId Long editorId, @PathVariable Long id,
                                 @RequestParam(required = false) Long after) {
        return feedback.thread(editorId, id, after);
    }

    /**
     * @param editorId signed-in admin or staff member
     * @param id       conversation
     * @param request  the reply
     * @return the saved reply
     */
    @PostMapping("/threads/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reply as the shop")
    public ChatMessageResponse reply(@CurrentUserId Long editorId, @PathVariable Long id,
                                     @Valid @RequestBody SendMessageRequest request) {
        return feedback.reply(editorId, id, request.body());
    }

    /**
     * @param editorId signed-in admin or staff member
     * @return average, count and latest ratings
     */
    @GetMapping("/ratings")
    @Operation(summary = "Order ratings summary")
    public RatingSummaryResponse ratings(@CurrentUserId Long editorId) {
        return feedback.ratingSummary(editorId);
    }
}
