package com.laundryhub.admin.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laundryhub.admin.api.dto.Problem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * JSON over HTTP for the admin console: sends the bearer token, parses records with Jackson and turns
 * problem-details error bodies into {@link ApiException}. All calls are asynchronous.
 */
public class ApiClient {

    /** Seconds to wait for the server. */
    public static final int TIMEOUT_SECONDS = 20;

    private static final String JSON = "application/json";
    private static final int FIRST_ERROR_STATUS = 300;
    private static final String NETWORK_MESSAGE = "Cannot reach the server. Check that the backend is running.";
    private static final String GENERIC_MESSAGE = "Something went wrong. Please try again.";

    // OOP: Encapsulation — HTTP client, JSON mapper and token source are private.
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper json;
    private final Supplier<String> token;

    /**
     * @param baseUrl API root ending in "/"
     * @param token   current bearer token, or {@code null} when signed out
     */
    public ApiClient(String baseUrl, Supplier<String> token) {
        if (baseUrl == null || !baseUrl.endsWith("/")) {
            throw new IllegalArgumentException("API base URL must end with '/'");
        }
        this.baseUrl = baseUrl;
        this.token = token;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
        this.json = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * @param path relative path, e.g. {@code admin/dashboard}
     * @param type response type
     * @param <T>  body type
     * @return the parsed body
     */
    public <T> CompletableFuture<T> get(String path, TypeReference<T> type) {
        return send(request(path).GET(), type);
    }

    /**
     * @param path relative path
     * @return the raw text body (CSV export)
     */
    public CompletableFuture<String> getText(String path) {
        return http.sendAsync(request(path).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, error) -> {
                    check(response, error);
                    return response.body();
                });
    }

    /**
     * @param method HTTP method (POST, PUT, PATCH, DELETE)
     * @param path   relative path
     * @param body   request body, or {@code null}
     * @param type   response type, or {@code null} for no body
     * @param <T>    body type
     * @return the parsed body ({@code null} for 204)
     */
    public <T> CompletableFuture<T> send(String method, String path, Object body, TypeReference<T> type) {
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8);
        return send(request(path).header("Content-Type", JSON).method(method, publisher), type);
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", JSON);
        String bearer = token == null ? null : token.get();
        if (bearer != null && !bearer.isBlank()) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return builder;
    }

    private <T> CompletableFuture<T> send(HttpRequest.Builder builder, TypeReference<T> type) {
        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, error) -> {
                    check(response, error);
                    String body = response.body();
                    if (type == null || body == null || body.isBlank()) {
                        return null;
                    }
                    return read(body, type);
                });
    }

    private void check(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            throw new ApiException(ApiException.NO_RESPONSE, NETWORK_MESSAGE, null);
        }
        if (response.statusCode() >= FIRST_ERROR_STATUS) {
            throw toException(response.statusCode(), response.body());
        }
    }

    /**
     * @param status HTTP status
     * @param body   problem-details JSON (may be blank)
     * @return the exception with the server's message, or a generic one
     */
    ApiException toException(int status, String body) {
        Problem problem = null;
        if (body != null && !body.isBlank()) {
            try {
                problem = json.readValue(body, Problem.class);
            } catch (JsonProcessingException e) {
                problem = null;
            }
        }
        String message = problem == null || problem.detail() == null || problem.detail().isBlank()
                ? GENERIC_MESSAGE : problem.detail();
        return new ApiException(status, message, problem == null ? null : problem.errors());
    }

    /**
     * @param text JSON
     * @param type target type
     * @param <T>  target
     * @return the parsed value
     */
    <T> T read(String text, TypeReference<T> type) {
        try {
            return json.readValue(text, type);
        } catch (IOException e) {
            throw new CompletionException(new ApiException(ApiException.NO_RESPONSE, GENERIC_MESSAGE, null));
        }
    }

    private String write(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write request body", e);
        }
    }
}
