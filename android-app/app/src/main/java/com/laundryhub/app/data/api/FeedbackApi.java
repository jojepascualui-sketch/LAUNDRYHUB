package com.laundryhub.app.data.api;

import com.laundryhub.app.data.api.dto.ChatDto;
import com.laundryhub.app.data.api.dto.ChatMessageDto;
import com.laundryhub.app.data.api.dto.RatingDto;
import com.laundryhub.app.data.api.dto.SendMessageDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Feedback chat (customers and riders) and order ratings (customers). */
// OOP: Abstraction — Retrofit generates the HTTP code behind this interface.
public interface FeedbackApi {

    /**
     * @param after last message id the app has, or {@code null} for the history
     * @return the conversation
     */
    @GET("feedback/messages")
    Call<ChatDto> messages(@Query("after") Long after);

    /**
     * @param body the message
     * @return the saved message
     */
    @POST("feedback/messages")
    Call<ChatMessageDto> send(@Body SendMessageDto body);

    /**
     * @param orderId the order
     * @return its rating, or 204 when not rated yet
     */
    @GET("orders/{id}/rating")
    Call<RatingDto> rating(@Path("id") long orderId);

    /**
     * @param orderId the completed order
     * @param body    stars and comment
     * @return the saved rating
     */
    @PUT("orders/{id}/rating")
    Call<RatingDto> rate(@Path("id") long orderId, @Body RatingDto body);
}
