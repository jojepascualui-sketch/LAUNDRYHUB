package com.laundryhub.backend.web;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.FeedbackService;
import com.laundryhub.backend.web.dto.ChatMessageResponse;
import com.laundryhub.backend.web.dto.ChatResponse;
import com.laundryhub.backend.web.dto.SendMessageRequest;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Feedback chat for customers and riders (no design yet). */
@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "Customer / rider ↔ shop chat")
public class FeedbackController {

    private final FeedbackService feedback;

    /**
     * @param feedback chat logic
     */
    public FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    /**
     * @param userId signed-in customer or rider
     * @param after  last message id the app has, or none for the history
     * @return the conversation (opening it marks replies read)
     */
    @GetMapping("/messages")
    @Operation(summary = "My conversation with the shop (poll with after=<last id>)")
    public ChatResponse messages(@CurrentUserId Long userId, @RequestParam(required = false) Long after) {
        return feedback.messagesFor(userId, after);
    }

    /**
     * @param userId  signed-in customer or rider
     * @param request the message
     * @return the saved message
     */
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message to the shop")
    public ChatMessageResponse send(@CurrentUserId Long userId, @Valid @RequestBody SendMessageRequest request) {
        return feedback.send(userId, request.body());
    }

    /**
     * @param userId signed-in customer or rider
     * @return {@code {"unread": n}} shop replies not opened yet
     */
    @GetMapping("/unread")
    @Operation(summary = "Unread shop replies (badge)")
    public Map<String, Integer> unread(@CurrentUserId Long userId) {
        return Map.of("unread", feedback.unreadFor(userId));
    }
}
