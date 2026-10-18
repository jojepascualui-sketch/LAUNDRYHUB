package com.laundryhub.app.data.api;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds {@code Authorization: Bearer <JWT>} to every request while signed in, and ends the
 * session when the server answers 401 to a request that carried a token.
 */
// OOP: Inheritance — implements OkHttp's Interceptor contract.
public class AuthInterceptor implements Interceptor {

    /** Header name. */
    public static final String AUTHORIZATION = "Authorization";

    /** Scheme prefix. */
    public static final String BEARER = "Bearer ";

    private static final int HTTP_UNAUTHORIZED = 401;

    // OOP: Encapsulation — the token source is fixed at construction.
    private final TokenProvider tokens;

    /**
     * @param tokens where the token comes from
     * @throws IllegalArgumentException if {@code tokens} is null
     */
    public AuthInterceptor(TokenProvider tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("Token provider is required");
        }
        this.tokens = tokens;
    }

    // OOP: Polymorphism — OkHttp calls this through the Interceptor interface.
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String token = tokens.getToken();
        Request request = chain.request();
        if (token != null && request.header(AUTHORIZATION) == null) {
            request = request.newBuilder().header(AUTHORIZATION, BEARER + token).build();
        }
        Response response = chain.proceed(request);
        if (response.code() == HTTP_UNAUTHORIZED && token != null) {
            tokens.onUnauthorized();
        }
        return response;
    }
}
