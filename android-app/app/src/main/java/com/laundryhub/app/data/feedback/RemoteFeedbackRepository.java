package com.laundryhub.app.data.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiCalls;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.FeedbackApi;
import com.laundryhub.app.data.api.dto.ChatDto;
import com.laundryhub.app.data.api.dto.ChatMessageDto;
import com.laundryhub.app.data.api.dto.RatingDto;
import com.laundryhub.app.data.api.dto.SendMessageDto;
import com.laundryhub.app.model.feedback.ChatMessage;
import com.laundryhub.app.model.feedback.Rating;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Calls {@code /api/feedback/**} and {@code /api/orders/{id}/rating}. */
// OOP: Inheritance — implements the FeedbackRepository contract.
public class RemoteFeedbackRepository implements FeedbackRepository {

    private static final int NO_CONTENT = 204;

    // OOP: Encapsulation — the Retrofit interface is fixed at construction.
    private final FeedbackApi api;

    /**
     * @param api feedback endpoints
     */
    public RemoteFeedbackRepository(@NonNull FeedbackApi api) {
        this.api = api;
    }

    // OOP: Polymorphism — the Retrofit-backed implementation of each FeedbackRepository method.
    @Override
    public void messages(@Nullable Long afterId, @NonNull ApiCallback<List<ChatMessage>> callback) {
        ApiCalls.enqueue(api.messages(afterId), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull ChatDto value) {
                List<ChatMessage> list = new ArrayList<>();
                for (ChatMessageDto dto : value.getMessages()) {
                    list.add(toMessage(dto));
                }
                callback.onSuccess(list);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void send(@NonNull String body, @NonNull ApiCallback<ChatMessage> callback) {
        ApiCalls.enqueue(api.send(new SendMessageDto(body)), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull ChatMessageDto value) {
                callback.onSuccess(toMessage(value));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void rating(long orderId, @NonNull ApiCallback<Optional<Rating>> callback) {
        // 204 = not rated yet; ApiCalls treats an empty body as an error, so this call is handled here.
        api.rating(orderId).enqueue(new Callback<RatingDto>() {
            @Override
            public void onResponse(@NonNull Call<RatingDto> call, @NonNull Response<RatingDto> response) {
                if (response.code() == NO_CONTENT || (response.isSuccessful() && response.body() == null)) {
                    callback.onSuccess(Optional.empty());
                } else if (response.isSuccessful()) {
                    RatingDto dto = response.body();
                    callback.onSuccess(Optional.of(new Rating(dto.getStars(), dto.getComment())));
                } else {
                    callback.onError(new ApiException(response.code(), null, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RatingDto> call, @NonNull Throwable t) {
                callback.onError(ApiException.network(t));
            }
        });
    }

    @Override
    public void rate(long orderId, @NonNull Rating rating, @NonNull ApiCallback<Rating> callback) {
        ApiCalls.enqueue(api.rate(orderId, new RatingDto(rating.stars(), rating.comment())), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull RatingDto value) {
                callback.onSuccess(new Rating(value.getStars(), value.getComment()));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    /**
     * @param dto server message
     * @return the app model
     */
    static ChatMessage toMessage(ChatMessageDto dto) {
        return new ChatMessage(dto.getId(), dto.isMine(), dto.getSenderName(), dto.getBody(),
                LocalDateTime.parse(dto.getSentAt()));
    }
}
