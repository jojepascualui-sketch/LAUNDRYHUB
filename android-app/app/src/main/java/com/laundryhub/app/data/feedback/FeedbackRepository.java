package com.laundryhub.app.data.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.model.feedback.ChatMessage;
import com.laundryhub.app.model.feedback.Rating;

import java.util.List;
import java.util.Optional;

/** Feedback chat with the shop and order ratings. */
// OOP: Abstraction — ViewModels depend on this interface; tests use a fake.
public interface FeedbackRepository {

    /**
     * @param afterId  last message id the screen has, or {@code null} for the history
     * @param callback newer messages, oldest first
     */
    void messages(@Nullable Long afterId, @NonNull ApiCallback<List<ChatMessage>> callback);

    /**
     * @param body     the message
     * @param callback the saved message
     */
    void send(@NonNull String body, @NonNull ApiCallback<ChatMessage> callback);

    /**
     * @param orderId  the order
     * @param callback the rating, or empty when not rated yet
     */
    void rating(long orderId, @NonNull ApiCallback<Optional<Rating>> callback);

    /**
     * @param orderId  a completed order
     * @param rating   stars and comment
     * @param callback the saved rating
     */
    void rate(long orderId, @NonNull Rating rating, @NonNull ApiCallback<Rating> callback);
}
