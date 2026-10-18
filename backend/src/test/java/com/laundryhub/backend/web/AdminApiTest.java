package com.laundryhub.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Admin console A1–A5. */
class AdminApiTest extends ApiTestBase {

    private static final String ADMIN = "/api/admin";

    @Test
    void dashboardCountsTodaysOrders() throws Exception {
        String customer = customer("ana@example.com");
        placeOrder(customer, deliveryOrder(10, "Hypoallergenic", NEAR_PIN, "GCASH")); // 410
        placeOrder(customer, walkInOrder(8, "Standard"));                                 // 240
        long cancelled = placeOrder(customer, walkInOrder(9, "Standard"));
        String admin = admin();
        patchJson(ADMIN + "/orders/" + cancelled, admin, Map.of("status", "CANCELLED", "cancelReason", "Duplicate"))
                .andExpect(status().isOk());

        getJson(ADMIN + "/dashboard", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersToday").value(3))
                .andExpect(jsonPath("$.waitingToProcess").value(2))
                .andExpect(jsonPath("$.kilosToday").value(18.00))
                .andExpect(jsonPath("$.salesToday").value(650.00))
                .andExpect(jsonPath("$.pipeline[0].label").value("Pending"))
                .andExpect(jsonPath("$.pipeline[0].count").value(2))
                .andExpect(jsonPath("$.recentOrders", hasSize(3)))
                .andExpect(jsonPath("$.recentOrders[0].orderNo").value("LH-0003"));
    }

    @Test
    void ordersCanBeFilteredAndSearched() throws Exception {
        String customer = customer("ana@example.com");
        long delivery = placeOrder(customer, deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        placeOrder(customer, walkInOrder(8, "Standard"));
        String staff = staff();
        patchJson(ADMIN + "/orders/" + delivery, staff, Map.of("status", "WASHING")).andExpect(status().isOk());

        getJson(ADMIN + "/orders", staff)
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.orders", hasSize(2)));
        getJson(ADMIN + "/orders?status=WASHING", staff)
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].orderNo").value("LH-0001"))
                .andExpect(jsonPath("$.counts[1].status").value("WASHING"))
                .andExpect(jsonPath("$.counts[1].count").value(1));
        getJson(ADMIN + "/orders?q=lh-0002", staff).andExpect(jsonPath("$.orders[0].orderNo").value("LH-0002"));
        getJson(ADMIN + "/orders?q=ana", staff).andExpect(jsonPath("$.orders", hasSize(2)));
        getJson(ADMIN + "/orders?status=WET", staff).andExpect(status().isBadRequest());
    }

    @Test
    void detailShowsWhatThePanelNeeds() throws Exception {
        long id = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Hypoallergenic", NEAR_PIN, "GCASH"));
        long walkIn = placeOrder(customer("juan@example.com"), walkInOrder(8, "Standard"));
        String staff = staff();

        getJson(ADMIN + "/orders/" + id, staff)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.customerName").value("Ana Reyes"))
                .andExpect(jsonPath("$.order.customerMobile").value("09170000001"))
                .andExpect(jsonPath("$.soapName").value("Hypoallergenic"))
                .andExpect(jsonPath("$.pickupLocation.mapsUrl").value(containsString("14.610000,121.010000")))
                .andExpect(jsonPath("$.price.total").value(410.00))
                .andExpect(jsonPath("$.payment.status").value("UNPAID"))
                .andExpect(jsonPath("$.statusOptions", hasSize(5)))
                .andExpect(jsonPath("$.jobs", hasSize(2)));
        getJson(ADMIN + "/orders/" + walkIn, staff)
                .andExpect(jsonPath("$.pickupLocation").doesNotExist())
                .andExpect(jsonPath("$.statusOptions", hasSize(4)))
                .andExpect(jsonPath("$.jobs", hasSize(0)));
        getJson(ADMIN + "/orders/999999", staff).andExpect(status().isNotFound());
    }

    @Test
    void statusRulesAreEnforced() throws Exception {
        long walkIn = placeOrder(customer("ana@example.com"), walkInOrder(8, "Standard"));
        long delivery = placeOrder(customer("juan@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        String staff = staff();

        patchJson(ADMIN + "/orders/" + walkIn, staff, Map.of("status", "OUT_FOR_DELIVERY"))
                .andExpect(status().isConflict());
        patchJson(ADMIN + "/orders/" + delivery, staff, Map.of("status", "CANCELLED"))
                .andExpect(status().isBadRequest());
        patchJson(ADMIN + "/orders/" + delivery, staff, Map.of("status", "CANCELLED", "cancelReason", "Customer request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("CANCELLED"))
                .andExpect(jsonPath("$.jobs[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.jobs[1].status").value("CANCELLED"));
        patchJson(ADMIN + "/orders/" + delivery, staff, Map.of("status", "WASHING")).andExpect(status().isConflict());
    }

    @Test
    void assignAndReassignRider() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        String one = rider("Rider One", "rider1@example.com", true);
        String two = rider("Rider Two", "rider2@example.com", false);
        String staff = staff();

        getJson(ADMIN + "/riders", staff).andExpect(jsonPath("$", hasSize(2)));
        patchJson(ADMIN + "/orders/" + order, staff, Map.of("riderId", idOf(one)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].type").value("PICKUP"))
                .andExpect(jsonPath("$.jobs[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.jobs[0].riderName").value("Rider One"));
        getJson("/api/rider/jobs/mine", one).andExpect(jsonPath("$", hasSize(1)));

        patchJson(ADMIN + "/orders/" + order, staff, Map.of("riderId", idOf(two), "jobType", "PICKUP"))
                .andExpect(jsonPath("$.jobs[0].riderName").value("Rider Two"));
        getJson("/api/rider/jobs/mine", one).andExpect(jsonPath("$", hasSize(0)));

        // Pre-assigning the return delivery: it goes straight to the rider when the order is Ready.
        patchJson(ADMIN + "/orders/" + order, staff, Map.of("riderId", idOf(one), "jobType", "RETURN_DELIVERY"))
                .andExpect(jsonPath("$.jobs[1].status").value("WAITING"));
        patchJson(ADMIN + "/orders/" + order, staff, Map.of("status", "READY"))
                .andExpect(jsonPath("$.jobs[1].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.jobs[1].riderName").value("Rider One"));
        patchJson(ADMIN + "/orders/" + order, staff, Map.of("riderId", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void paymentCanBeConfirmedFromTheConsole() throws Exception {
        long gcash = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "GCASH"));
        long cash = placeOrder(customer("juan@example.com"), walkInOrder(8, "Standard"));
        String staff = staff();

        postJson(ADMIN + "/orders/" + gcash + "/payment", staff, Map.of()).andExpect(status().isBadRequest());
        postJson(ADMIN + "/orders/" + gcash + "/payment", staff, Map.of("referenceNo", "GC-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.paid").value(true))
                .andExpect(jsonPath("$.payment.referenceNo").value("GC-123456"));
        postJson(ADMIN + "/orders/" + gcash + "/payment", staff, Map.of("referenceNo", "GC-123456"))
                .andExpect(status().isConflict());
        postJson(ADMIN + "/orders/" + cash + "/payment", staff, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.status").value("PAID"));
    }

    @Test
    void adminChangesPricesForNewOrdersOnly() throws Exception {
        String customer = customer("ana@example.com");
        long before = placeOrder(customer, walkInOrder(8, "Standard"));
        String admin = admin();
        Map<String, Object> form = new HashMap<>();
        form.put("ratePerKg", 35);
        form.put("minKg", 6);
        form.put("deliveryFee", 50);
        form.put("deliveryRadiusKm", 3);
        form.put("cashEnabled", true);
        form.put("gcashEnabled", true);
        form.put("shopGcashNumber", "09170009999");

        putJson(ADMIN + "/pricing", staff(), form).andExpect(status().isForbidden());
        putJson(ADMIN + "/pricing", admin, form)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerKg").value(35.00))
                .andExpect(jsonPath("$.soaps", hasSize(4)));

        postJson("/api/orders/quote", customer, Map.of("serviceType", "PICKUP_DELIVERY", "weightKg", 6,
                "soapId", soapId("Standard")))
                .andExpect(jsonPath("$.total").value(260.00));
        getJson("/api/orders/" + before, customer).andExpect(jsonPath("$.price.total").value(240.00));
        // The 3 km radius now rejects a pin about 3.2 km away.
        postJson("/api/orders", customer, deliveryOrder(8, "Standard", FARTHER_PIN, "CASH"))
                .andExpect(status().isBadRequest());

        form.put("shopGcashNumber", null);
        putJson(ADMIN + "/pricing", admin, form)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Shop GCash number is required when GCash is enabled"));
        form.put("ratePerKg", -1);
        putJson(ADMIN + "/pricing", admin, form)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ratePerKg").exists());
    }

    @Test
    void soapCrud() throws Exception {
        String admin = admin();
        Map<String, Object> soap = new HashMap<>(Map.of("name", "Eco Clean", "description", "Plant-based formula",
                "addOnPerKg", 4, "available", true));

        Number id = read(postJson(ADMIN + "/soaps", admin, soap)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.tintHex").value("#E4EEF8")), "$.id");
        postJson(ADMIN + "/soaps", admin, soap).andExpect(status().isConflict());
        getJson("/api/pricing", null).andExpect(jsonPath("$.soaps", hasSize(5)));

        soap.put("available", false);
        soap.put("addOnPerKg", 3.5);
        putJson(ADMIN + "/soaps/" + id, admin, soap)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addOnPerKg").value(3.50))
                .andExpect(jsonPath("$.available").value(false));
        getJson("/api/pricing", null).andExpect(jsonPath("$.soaps", hasSize(4)));
        getJson(ADMIN + "/soaps", admin).andExpect(jsonPath("$", hasSize(5)));
        postJson("/api/orders/quote", customer("ana@example.com"),
                Map.of("serviceType", "WALK_IN", "weightKg", 8, "soapId", id))
                .andExpect(status().isBadRequest());

        call(delete(ADMIN + "/soaps/" + id), admin, null).andExpect(status().isNoContent());
        placeOrder(customer("juan@example.com"), walkInOrder(8, "Baby-Safe"));
        call(delete(ADMIN + "/soaps/" + soapId("Baby-Safe")), admin, null).andExpect(status().isConflict());
    }

    @Test
    void userCrud() throws Exception {
        String admin = admin();
        String customer = customer("ana@example.com");
        placeOrder(customer, walkInOrder(8, "Standard"));

        Map<String, Object> newRider = new HashMap<>(Map.of("role", "RIDER", "fullName", "Carlo Mendoza",
                "email", "carlo@example.com", "mobileNumber", "09190001234", "password", "rider-pass-1"));
        postJson(ADMIN + "/users", admin, newRider)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Plate number must not be blank"));
        newRider.put("plateNumber", "abc 1234");
        Number riderId = read(postJson(ADMIN + "/users", admin, newRider)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("ABC 1234"))
                .andExpect(jsonPath("$.onDuty").value(false)), "$.id");
        postJson("/api/auth/login", null, Map.of("email", "carlo@example.com", "password", "rider-pass-1", "role", "RIDER"))
                .andExpect(status().isOk());
        postJson(ADMIN + "/users", admin, Map.of("role", "STAFF", "fullName", "Liza Gonzaga",
                "email", "liza@laundryhub.test", "position", "Counter staff"))
                .andExpect(status().isCreated());

        getJson(ADMIN + "/users?role=CUSTOMER", admin)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderCount").value(1));
        getJson(ADMIN + "/users?role=RIDER", admin).andExpect(jsonPath("$[0].plateNumber").value("ABC 1234"));
        getJson(ADMIN + "/users?role=STAFF&role=ADMIN", admin)
                .andExpect(jsonPath("$[*].position", contains("Owner", "Counter staff")));

        putJson(ADMIN + "/users/" + riderId, admin, Map.of("fullName", "Carlo M. Mendoza", "status", "INACTIVE",
                "plateNumber", "XYZ 9876"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.plateNumber").value("XYZ 9876"));
        postJson("/api/auth/login", null, Map.of("email", "carlo@example.com", "password", "rider-pass-1"))
                .andExpect(status().isUnauthorized());

        call(delete(ADMIN + "/users/" + idOf(customer)), admin, null).andExpect(status().isNoContent());
        getJson("/api/orders/mine", customer).andExpect(status().isUnauthorized());
        call(delete(ADMIN + "/users/" + idOf(admin)), admin, null).andExpect(status().isConflict());
        postJson(ADMIN + "/users", admin, Map.of("role", "CUSTOMER", "fullName", "Dup", "email", "ANA@example.com"))
                .andExpect(status().isConflict());
    }

    @Test
    void reportsSplitSalesAndExportCsv() throws Exception {
        String customer = customer("ana@example.com");
        placeOrder(customer, deliveryOrder(10, "Hypoallergenic", NEAR_PIN, "GCASH")); // 410 GCash delivery
        placeOrder(customer, walkInOrder(8, "Standard"));                                 // 240 cash walk-in
        placeOrder(customer, walkInOrder(9, "Hypoallergenic"));                           // 315 cash walk-in
        String admin = admin();
        String today = LocalDate.now().toString();

        getJson(ADMIN + "/reports?from=" + today + "&to=" + today, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(3))
                .andExpect(jsonPath("$.totalSales").value(965.00))
                .andExpect(jsonPath("$.cashSales").value(555.00))
                .andExpect(jsonPath("$.gcashSales").value(410.00))
                .andExpect(jsonPath("$.deliveryCount").value(1))
                .andExpect(jsonPath("$.walkInCount").value(2))
                .andExpect(jsonPath("$.soapSales[0].soapName").value("Hypoallergenic"))
                .andExpect(jsonPath("$.soapSales[0].sales").value(725.00))
                .andExpect(jsonPath("$.soapSales[1].soapName").value("Standard"));
        getJson(ADMIN + "/reports?from=" + today + "&to=2000-01-01", admin).andExpect(status().isBadRequest());

        String csv = getJson(ADMIN + "/reports/export.csv?from=" + today + "&to=" + today, admin)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("laundryhub-sales-")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<String> lines = csv.lines().toList();
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).startsWith("﻿Order,Placed,Customer,Service");
        assertThat(lines.get(1)).startsWith("LH-0001,").contains("Pickup & Delivery", "GCash", "410.00");
    }
}
