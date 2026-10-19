package com.laundryhub.app.data.api;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the Retrofit client: base URL from BuildConfig, JSON via Gson, JWT via {@link AuthInterceptor}. */
public final class ApiClient {

    /** Seconds to wait for connect / read before failing. */
    public static final int TIMEOUT_SECONDS = 20;

    // OOP: Encapsulation — one Retrofit instance, exposed only as typed API interfaces.
    private final Retrofit retrofit;

    /**
     * @param baseUrl API root ending in "/", e.g. {@code BuildConfig.API_BASE_URL}
     * @param tokens  token source for the JWT interceptor
     * @throws IllegalArgumentException if the base URL is blank or does not end with "/"
     */
    public ApiClient(@NonNull String baseUrl, @NonNull TokenProvider tokens) {
        if (baseUrl.isEmpty() || !baseUrl.endsWith("/")) {
            throw new IllegalArgumentException("API base URL must end with '/'");
        }
        OkHttpClient http = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokens))
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /** @return pricing and shop endpoints */
    @NonNull
    public CatalogApi catalog() {
        return retrofit.create(CatalogApi.class);
    }

    /** @return customer order endpoints */
    @NonNull
    public OrderApi orders() {
        return retrofit.create(OrderApi.class);
    }

    /** @return feedback chat and order ratings */
    @NonNull
    public FeedbackApi feedback() {
        return retrofit.create(FeedbackApi.class);
    }

    /** @return rider bookings, jobs and GPS */
    @NonNull
    public RiderApi rider() {
        return retrofit.create(RiderApi.class);
    }

    /** @return the auth endpoints */
    @NonNull
    public AuthApi auth() {
        return retrofit.create(AuthApi.class);
    }
}
