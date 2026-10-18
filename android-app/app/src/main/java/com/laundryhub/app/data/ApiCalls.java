package com.laundryhub.app.data;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.laundryhub.app.data.api.dto.ProblemDetail;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Runs Retrofit calls and turns error bodies into {@link ApiException}. */
public final class ApiCalls {

    private static final Gson GSON = new Gson();

    private ApiCalls() {
    }

    /**
     * Runs the call asynchronously; Retrofit delivers the callback on the main thread.
     *
     * @param call     the call
     * @param callback receives the body or the error
     * @param <T>      body type
     */
    public static <T> void enqueue(@NonNull Call<T> call, @NonNull ApiCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> c, @NonNull Response<T> response) {
                T body = response.body();
                if (response.isSuccessful() && body != null) {
                    callback.onSuccess(body);
                } else {
                    callback.onError(toException(response.code(), response.errorBody()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<T> c, @NonNull Throwable t) {
                callback.onError(ApiException.network(t));
            }
        });
    }

    /**
     * Reads a problem-details error body.
     *
     * @param status HTTP status
     * @param body   error body, may be {@code null}
     * @return the exception (message is {@code null} if the body was not problem JSON)
     */
    @NonNull
    static ApiException toException(int status, ResponseBody body) {
        if (body == null) {
            return new ApiException(status, null, null);
        }
        try {
            ProblemDetail problem = GSON.fromJson(body.charStream(), ProblemDetail.class);
            return problem == null ? new ApiException(status, null, null)
                    : new ApiException(status, problem.getDetail(), problem.getErrors());
        } catch (JsonParseException | IllegalStateException e) {
            return new ApiException(status, null, null);
        } finally {
            body.close();
        }
    }

    /**
     * Same as {@link #toException(int, ResponseBody)} for a raw JSON string (tests).
     *
     * @param status HTTP status
     * @param json   body text
     * @return the exception
     * @throws IOException never
     */
    @NonNull
    static ApiException toException(int status, String json) throws IOException {
        return toException(status, ResponseBody.create(json, okhttp3.MediaType.get("application/problem+json")));
    }
}
