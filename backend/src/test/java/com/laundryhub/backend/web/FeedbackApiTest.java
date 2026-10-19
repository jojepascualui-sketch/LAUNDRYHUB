package com.laundryhub.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Feedback chat (customer / rider ↔ shop) and order ratings, end to end. */
class FeedbackApiTest extends ApiTestBase {

    private static final String MESSAGES = "/api/feedback/messages";
    private static final String THREADS = "/api/admin/feedback/threads";

    @Test
    void customerAndShopTalkWithUnreadCounters() throws Exception {
        String ana = customer("ana@example.com");
        String admin = admin();

        // No conversation yet.
        getJson(MESSAGES, ana).andExpect(status().isOk()).andExpect(jsonPath("$.threadId").doesNotExist())
                .andExpect(jsonPath("$.messages").isEmpty());

        postJson(MESSAGES, ana, Map.of("body", "  Can I add 2 kg to my order?  ")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value("USER"))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.body").value("Can I add 2 kg to my order?"));

        // The console sees the conversation with one unread message.
        getJson(THREADS, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("CUSTOMER"))
                .andExpect(jsonPath("$[0].unread").value(1))
                .andExpect(jsonPath("$[0].lastMessage").value("Can I add 2 kg to my order?"));
        Number threadId = read(getJson(THREADS, admin), "$[0].id");

        // Opening it marks it read; the shop replies.
        getJson(THREADS + "/" + threadId + "/messages", admin).andExpect(jsonPath("$.messages[0].mine").value(false));
        getJson(THREADS, admin).andExpect(jsonPath("$[0].unread").value(0));
        postJson(THREADS + "/" + threadId + "/messages", admin, Map.of("body", "Yes, tell the rider at pickup."))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.side").value("SHOP"));

        // The customer has one unread reply and sees both messages in order.
        getJson("/api/feedback/unread", ana).andExpect(jsonPath("$.unread").value(1));
        Number firstId = read(getJson(MESSAGES, ana), "$.messages[0].id");
        getJson(MESSAGES, ana)
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[1].side").value("SHOP"))
                .andExpect(jsonPath("$.messages[1].mine").value(false))
                .andExpect(jsonPath("$.messages[1].senderName").value("Admin Owner"));
        getJson("/api/feedback/unread", ana).andExpect(jsonPath("$.unread").value(0));

        // Polling with after= returns only newer messages.
        getJson(MESSAGES + "?after=" + firstId, ana).andExpect(jsonPath("$.messages.length()").value(1));
    }

    @Test
    void ridersHaveTheirOwnConversation() throws Exception {
        String rider = rider("Rider One", "rider1@example.com", true);
        String ana = customer("ana@example.com");

        postJson(MESSAGES, rider, Map.of("body", "Gate at LH-0003 is locked.")).andExpect(status().isCreated());
        getJson(MESSAGES, ana).andExpect(jsonPath("$.messages").isEmpty());
        String admin = admin();
        getJson(THREADS, admin).andExpect(jsonPath("$[0].role").value("RIDER"))
                .andExpect(jsonPath("$[0].userName").value("Rider One"));
    }

    @Test
    void accessRulesAndValidation() throws Exception {
        String ana = customer("ana@example.com");
        String bea = customer("bea@example.com");
        String admin = admin();
        postJson(MESSAGES, ana, Map.of("body", "Hello")).andExpect(status().isCreated());
        Number threadId = read(getJson(THREADS, admin), "$[0].id");

        // Console users answer in the console, not through the app endpoints.
        getJson(MESSAGES, admin).andExpect(status().isForbidden());
        // Customers cannot read the console endpoints or someone else's conversation.
        getJson(THREADS + "/" + threadId + "/messages", bea).andExpect(status().isForbidden());
        getJson(MESSAGES, bea).andExpect(jsonPath("$.messages").isEmpty());
        // Without a token.
        getJson(MESSAGES, null).andExpect(status().isUnauthorized());
        // Blank and too-long messages.
        postJson(MESSAGES, ana, Map.of("body", "   ")).andExpect(status().isBadRequest());
        postJson(MESSAGES, ana, Map.of("body", "x".repeat(1001))).andExpect(status().isBadRequest());
        // Unknown conversation.
        getJson(THREADS + "/999999/messages", admin).andExpect(status().isNotFound());
    }

    @Test
    void customersRateCompletedOrdersOnly() throws Exception {
        String ana = customer("ana@example.com");
        long orderId = placeOrder(ana, walkInOrder(8, "Standard"));
        String rating = "/api/orders/" + orderId + "/rating";

        getJson(rating, ana).andExpect(status().isNoContent());
        putJson(rating, ana, Map.of("stars", 5)).andExpect(status().isConflict());

        jdbc.update("UPDATE orders SET status = 'COMPLETED' WHERE id = ?", orderId);
        putJson(rating, ana, Map.of("stars", 6)).andExpect(status().isBadRequest());
        putJson(rating, ana, Map.of("stars", 4, "comment", "Clean and on time"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stars").value(4));
        // Changing the rating keeps one row.
        putJson(rating, ana, Map.of("stars", 5)).andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.comment").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_ratings", Integer.class)).isEqualTo(1);

        // Another customer cannot see or rate it.
        String bea = customer("bea@example.com");
        getJson(rating, bea).andExpect(status().isNotFound());

        // The console sees the average and the latest ratings.
        String admin = admin();
        getJson("/api/admin/feedback/ratings", admin)
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.latest[0].stars").value(5));
        List<?> latest = read(getJson("/api/admin/feedback/ratings", admin), "$.latest");
        assertThat(latest).hasSize(1);
    }
}
