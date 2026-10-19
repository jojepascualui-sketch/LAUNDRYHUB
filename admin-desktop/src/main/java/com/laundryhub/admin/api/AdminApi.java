package com.laundryhub.admin.api;

import com.laundryhub.admin.api.dto.AuthResponse;
import com.laundryhub.admin.api.dto.Chat;
import com.laundryhub.admin.api.dto.ChatMessage;
import com.laundryhub.admin.api.dto.ChatThread;
import com.laundryhub.admin.api.dto.RatingSummary;
import com.laundryhub.admin.api.dto.Dashboard;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Everything the admin console asks the backend (docs/SCREEN_API_MAP.md, A1–A5). */
// OOP: Abstraction — controllers depend on this interface, not on HTTP.
public interface AdminApi {

    /**
     * @param email    staff email
     * @param password password
     * @return token and profile (admin or staff only)
     */
    CompletableFuture<AuthResponse> signIn(String email, String password);

    /** @return A1 numbers, pipeline and recent orders */
    CompletableFuture<Dashboard> dashboard();

    /**
     * @param status filter, or {@code null} for all
     * @param query  order no. or customer, or {@code null}
     * @return A2 list with counts per status
     */
    CompletableFuture<OrderList> orders(OrderStatus status, String query);

    /**
     * @param id order id
     * @return A2 detail panel
     */
    CompletableFuture<OrderDetail> order(long id);

    /**
     * @param id     order id
     * @param update new status and/or rider
     * @return the updated detail
     */
    CompletableFuture<OrderDetail> updateOrder(long id, OrderUpdate update);

    /** @return riders for "Assign rider" */
    CompletableFuture<List<RiderOption>> riders();

    /** @return A3 rates, payment methods and soaps */
    CompletableFuture<PricingSettings> pricing();

    /**
     * @param update new rates and payment settings
     * @return the saved settings
     */
    CompletableFuture<PricingSettings> savePricing(PricingUpdate update);

    /**
     * @param soap new soap
     * @return the created soap
     */
    CompletableFuture<Soap> createSoap(SoapRequest soap);

    /**
     * @param id   soap id
     * @param soap new values
     * @return the saved soap
     */
    CompletableFuture<Soap> updateSoap(long id, SoapRequest soap);

    /**
     * @param role customers, riders or console users (admin + staff)
     * @return A4 table rows
     */
    CompletableFuture<List<User>> users(List<Role> role);

    /**
     * @param user new account
     * @return the created account
     */
    CompletableFuture<User> createUser(UserCreate user);

    /**
     * @param id   account id
     * @param user new values
     * @return the saved account
     */
    CompletableFuture<User> updateUser(long id, UserUpdate user);

    /** @return A5 totals and sales by soap (all time) */
    CompletableFuture<Report> report();

    /** @return A5 "Export CSV" text */
    CompletableFuture<String> exportCsv();

    /** @return Feedback: conversations with customers and riders, newest activity first */
    CompletableFuture<List<ChatThread>> feedbackThreads();

    /**
     * @param threadId conversation
     * @param afterId  last message id shown, or {@code null} for the history
     * @return newer messages (opening marks them read)
     */
    CompletableFuture<Chat> feedbackMessages(long threadId, Long afterId);

    /**
     * @param threadId conversation
     * @param body     the reply
     * @return the saved reply
     */
    CompletableFuture<ChatMessage> reply(long threadId, String body);

    /** @return average, count and latest order ratings */
    CompletableFuture<RatingSummary> ratings();
}
