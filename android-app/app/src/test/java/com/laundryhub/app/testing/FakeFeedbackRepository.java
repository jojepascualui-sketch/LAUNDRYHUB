package com.laundryhub.app.testing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.feedback.FeedbackRepository;
import com.laundryhub.app.model.feedback.ChatMessage;
import com.laundryhub.app.model.feedback.Rating;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory feedback backend: answers synchronously; the shop can "reply" with {@link #shopReplies}. */
public class FakeFeedbackRepository implements FeedbackRepository {

    public final List<ChatMessage> messages = new ArrayList<>();
    public final Map<Long, Rating> ratings = new HashMap<>();
    /** When set, every call fails with this status. */
    public Integer failWith;
    /** The "after" value of the last messages() call. */
    public Long lastAfter;
    private long nextId = 1;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 19, 14, 0);

    /**
     * @param body the shop's reply
     */
    public void shopReplies(String body) {
        messages.add(new ChatMessage(nextId, false, "Admin Owner", body, BASE.plusMinutes(nextId)));
        nextId++;
    }

    private boolean failed(ApiCallback<?> callback) {
        if (failWith != null) {
            callback.onError(new ApiException(failWith, "Server error " + failWith, null));
            return true;
        }
        return false;
    }

    @Override
    public void messages(@Nullable Long afterId, @NonNull ApiCallback<List<ChatMessage>> callback) {
        lastAfter = afterId;
        if (failed(callback)) {
            return;
        }
        List<ChatMessage> newer = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (afterId == null || message.id() > afterId) {
                newer.add(message);
            }
        }
        callback.onSuccess(newer);
    }

    @Override
    public void send(@NonNull String body, @NonNull ApiCallback<ChatMessage> callback) {
        if (failed(callback)) {
            return;
        }
        ChatMessage message = new ChatMessage(nextId, true, "Ana Reyes", body, BASE.plusMinutes(nextId));
        nextId++;
        messages.add(message);
        callback.onSuccess(message);
    }

    @Override
    public void rating(long orderId, @NonNull ApiCallback<Optional<Rating>> callback) {
        if (!failed(callback)) {
            callback.onSuccess(Optional.ofNullable(ratings.get(orderId)));
        }
    }

    @Override
    public void rate(long orderId, @NonNull Rating rating, @NonNull ApiCallback<Rating> callback) {
        if (!failed(callback)) {
            ratings.put(orderId, rating);
            callback.onSuccess(rating);
        }
    }
}
