package com.laundryhub.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * End to end through the HTTP API: a customer places a Pickup &amp; Delivery order, riders compete
 * for it, and the jobs move the order from Pending to Completed (DESIGN_SPEC R1–R4).
 */
class RiderJobLifecycleApiTest extends ApiTestBase {

    private static final String RIDER = "/api/rider";

    private String accept(long jobId) {
        return RIDER + "/jobs/" + jobId + "/accept";
    }

    private String advance(long jobId) {
        return RIDER + "/jobs/" + jobId + "/advance";
    }

    @Test
    void orderGoesFromPlacedToDeliveredWithTwoRiders() throws Exception {
        String customer = customer("ana@example.com");
        String admin = admin();
        long order = placeOrder(customer, deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        placeOrder(customer, walkInOrder(8, "Standard")); // a walk-in never reaches riders
        long pickup = jobId(order, "PICKUP");
        long delivery = jobId(order, "RETURN_DELIVERY");
        String one = rider("Rider One", "rider1@example.com", true);
        String two = rider("Rider Two", "rider2@example.com", false);

        // R1: only on-duty riders see the pickup; the return delivery waits for Ready.
        getJson(RIDER + "/jobs/available", one)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(pickup))
                .andExpect(jsonPath("$[0].typeLabel").value("Pickup"));
        getJson(RIDER + "/jobs/available", two).andExpect(jsonPath("$", hasSize(0)));
        postJson(accept(pickup), two, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Go on duty to accept bookings."));
        patchJson(RIDER + "/duty", two, Map.of("onDuty", true)).andExpect(status().isOk());

        // One winner: the second rider gets 409 and cannot see, advance or decline the job.
        postJson(accept(pickup), one, null).andExpect(status().isOk());
        postJson(accept(pickup), two, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This booking was already taken by another rider."));
        postJson(advance(pickup), two, null).andExpect(status().isNotFound());
        postJson(RIDER + "/jobs/" + pickup + "/decline", two, null).andExpect(status().isConflict());
        getJson(RIDER + "/jobs/" + pickup, two).andExpect(status().isNotFound());

        // R2 pickup. A lighter load (5 kg) is still billed as 8 kg: 8 × 30 + 60.
        postJson(advance(pickup), one, null).andExpect(jsonPath("$.currentActionLabel").value("I’ve arrived"));
        postJson(advance(pickup), one, null).andExpect(jsonPath("$.currentActionLabel").value("Confirm pickup"));
        postJson(advance(pickup), one, Map.of("weighedKg", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billableKg").value(8.00))
                .andExpect(jsonPath("$.total").value(300.00));
        assertThat(orderStatus(order)).isEqualTo("PENDING");
        postJson(advance(pickup), one, null).andExpect(jsonPath("$.status").value("COMPLETED"));
        postJson(advance(pickup), one, null).andExpect(status().isConflict());
        assertThat(orderStatus(order)).isEqualTo("WASHING");

        // The return delivery is offered to every on-duty rider once the order is Ready.
        getJson(RIDER + "/jobs/available", two).andExpect(jsonPath("$", hasSize(0)));
        patchJson("/api/admin/orders/" + order, admin, Map.of("status", "READY")).andExpect(status().isOk());
        getJson(RIDER + "/jobs/available", one).andExpect(jsonPath("$", hasSize(1)));
        getJson(RIDER + "/jobs/available", two)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(delivery))
                .andExpect(jsonPath("$[0].payment.amountDue").value(300.00));

        // R3 with cash: rider two takes it this time.
        postJson(accept(delivery), two, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps", hasSize(5)))
                .andExpect(jsonPath("$.steps[3].title").value("Cash collected"));
        postJson(advance(delivery), two, null).andExpect(status().isOk());
        assertThat(orderStatus(order)).isEqualTo("OUT_FOR_DELIVERY");
        postJson(advance(delivery), two, null).andExpect(status().isOk());
        postJson(advance(delivery), two, null).andExpect(jsonPath("$.awaitingCash").value(true));
        postJson(RIDER + "/jobs/" + delivery + "/cash-collected", one, null).andExpect(status().isNotFound());
        postJson(advance(delivery), two, null).andExpect(status().isConflict());
        postJson(RIDER + "/jobs/" + delivery + "/cash-collected", two, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.paid").value(true))
                .andExpect(jsonPath("$.payment.amountDue").value(0));
        postJson(advance(delivery), two, null).andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(orderStatus(order)).isEqualTo("COMPLETED");
        getJson("/api/orders/" + order, customer)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payment.status").value("PAID"))
                .andExpect(jsonPath("$.price.total").value(300.00));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rider_jobs", Integer.class)).isEqualTo(2);
        getJson(RIDER + "/summary", one).andExpect(jsonPath("$.doneTodayCount").value(1));
        getJson(RIDER + "/summary", two).andExpect(jsonPath("$.doneTodayCount").value(1));
    }

    @Test
    void aJobCannotBeAdvancedBeforeItIsAccepted() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        String rider = rider("Rider One", "rider1@example.com", true);

        postJson(advance(jobId(order, "PICKUP")), rider, null).andExpect(status().isNotFound());
        postJson(accept(jobId(order, "RETURN_DELIVERY")), rider, null).andExpect(status().isConflict());
        postJson(accept(999_999L), rider, null).andExpect(status().isNotFound());
    }

    @Test
    void weightIsOnlyAcceptedWithConfirmPickup() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "GCASH"));
        long pickup = jobId(order, "PICKUP");
        String rider = rider("Rider One", "rider1@example.com", true);
        postJson(accept(pickup), rider, null).andExpect(status().isOk());

        postJson(advance(pickup), rider, Map.of("weighedKg", 11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The weight can only be sent with \"Confirm pickup\"."));
        getJson("/api/rider/jobs/" + pickup, rider).andExpect(jsonPath("$.currentStepIndex").value(0));
    }

    @Test
    void cancelledOrdersDisappearFromTheBookings() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        String rider = rider("Rider One", "rider1@example.com", true);
        getJson(RIDER + "/jobs/available", rider).andExpect(jsonPath("$", hasSize(1)));

        patchJson("/api/admin/orders/" + order, admin(), Map.of("status", "CANCELLED", "cancelReason", "Duplicate"))
                .andExpect(status().isOk());

        getJson(RIDER + "/jobs/available", rider).andExpect(jsonPath("$", hasSize(0)));
        postJson(accept(jobId(order, "PICKUP")), rider, null).andExpect(status().isConflict());
    }
}
