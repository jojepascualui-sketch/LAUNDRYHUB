package com.laundryhub.admin.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.laundryhub.admin.api.dto.AuthResponse;
import com.laundryhub.admin.api.dto.Chat;
import com.laundryhub.admin.api.dto.ChatMessage;
import com.laundryhub.admin.api.dto.ChatThread;
import com.laundryhub.admin.api.dto.RatingSummary;
import com.laundryhub.admin.api.dto.SendMessage;
import com.laundryhub.admin.api.dto.Dashboard;
import com.laundryhub.admin.api.dto.LoginRequest;
import com.laundryhub.admin.api.dto.OrderDetail;
import com.laundryhub.admin.api.dto.OrderList;
import com.laundryhub.admin.api.dto.OrderUpdate;
import com.laundryhub.admin.api.dto.PricingSettings;
import com.laundryhub.admin.api.dto.PricingUpdate;
import com.laundryhub.admin.api.dto.Report;
import com.laundryhub.admin.api.dto.RiderOption;
import com.laundryhub.admin.api.dto.Soap;
import com.laundryhub.admin.api.dto.SoapRequest;
import com.laundryhub.admin.api.dto.User;
import com.laundryhub.admin.api.dto.UserCreate;
import com.laundryhub.admin.api.dto.UserUpdate;
import com.laundryhub.admin.model.OrderStatus;
import com.laundryhub.admin.model.Role;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/** {@link AdminApi} over the Spring Boot REST API. */
// OOP: Inheritance — implements the AdminApi contract.
public class HttpAdminApi implements AdminApi {

    private static final String PATCH = "PATCH";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String FEEDBACK = "admin/feedback/";

    // OOP: Encapsulation — the HTTP client stays private.
    private final ApiClient client;

    /**
     * @param client JSON client
     */
    public HttpAdminApi(ApiClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client is required");
        }
        this.client = client;
    }

    // OOP: Polymorphism — the HTTP implementation of each AdminApi call.
    @Override
    public CompletableFuture<AuthResponse> signIn(String email, String password) {
        return client.send(POST, "auth/login", new LoginRequest(email, password, Role.ADMIN.name()),
                new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<Dashboard> dashboard() {
        return client.get("admin/dashboard", new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<OrderList> orders(OrderStatus status, String query) {
        StringJoiner params = new StringJoiner("&", "?", "").setEmptyValue("");
        if (status != null) {
            params.add("status=" + status.name());
        }
        if (query != null && !query.isBlank()) {
            params.add("q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        }
        return client.get("admin/orders" + params, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<OrderDetail> order(long id) {
        return client.get("admin/orders/" + id, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<OrderDetail> updateOrder(long id, OrderUpdate update) {
        return client.send(PATCH, "admin/orders/" + id, update, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<List<RiderOption>> riders() {
        return client.get("admin/riders", new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<PricingSettings> pricing() {
        return client.get("admin/pricing", new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<PricingSettings> savePricing(PricingUpdate update) {
        return client.send(PUT, "admin/pricing", update, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<Soap> createSoap(SoapRequest soap) {
        return client.send(POST, "admin/soaps", soap, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<Soap> updateSoap(long id, SoapRequest soap) {
        return client.send(PUT, "admin/soaps/" + id, soap, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<List<User>> users(List<Role> roles) {
        StringJoiner params = new StringJoiner("&", "?", "").setEmptyValue("");
        for (Role role : roles) {
            params.add("role=" + role.name());
        }
        return client.get("admin/users" + params, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<User> createUser(UserCreate user) {
        return client.send(POST, "admin/users", user, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<User> updateUser(long id, UserUpdate user) {
        return client.send(PUT, "admin/users/" + id, user, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<Report> report() {
        return client.get("admin/reports", new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<String> exportCsv() {
        return client.getText("admin/reports/export.csv");
    }

    @Override
    public CompletableFuture<List<ChatThread>> feedbackThreads() {
        return client.get(FEEDBACK + "threads", new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<Chat> feedbackMessages(long threadId, Long afterId) {
        String after = afterId == null ? "" : "?after=" + afterId;
        return client.get(FEEDBACK + "threads/" + threadId + "/messages" + after, new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<ChatMessage> reply(long threadId, String body) {
        return client.send(POST, FEEDBACK + "threads/" + threadId + "/messages", new SendMessage(body),
                new TypeReference<>() { });
    }

    @Override
    public CompletableFuture<RatingSummary> ratings() {
        return client.get(FEEDBACK + "ratings", new TypeReference<>() { });
    }
}
