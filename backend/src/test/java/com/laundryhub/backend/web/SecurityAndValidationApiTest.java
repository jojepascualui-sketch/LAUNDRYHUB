package com.laundryhub.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Role-based access per area (401 / 403 as ProblemDetail), sign-in per tab and 400 field errors. */
class SecurityAndValidationApiTest extends ApiTestBase {

    private static final String CUSTOMER_AREA = "/api/orders/mine";
    private static final String RIDER_AREA = "/api/rider/summary";
    private static final String ADMIN_AREA = "/api/admin/dashboard";
    private static final String SIGN_IN = "Please sign in.";
    private static final String NO_ACCESS = "You do not have access to this.";

    private Map<String, Object> login(String email, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", PASSWORD);
        if (role != null) {
            body.put("role", role);
        }
        return body;
    }

    // ---- CORS (website) -----------------------------------------------------------------------

    @Test
    void theWebsiteOriginMayCallTheApiAndOtherOriginsMayNot() throws Exception {
        String website = "http://localhost:5173";
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, website)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, website));
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    // ---- 401 / 403 ----------------------------------------------------------------------------

    @Test
    void everyProtectedAreaAnswers401AsProblemDetailWithoutAToken() throws Exception {
        for (String url : List.of(CUSTOMER_AREA, RIDER_AREA, ADMIN_AREA, "/api/auth/me", "/api/geocode/reverse")) {
            getJson(url, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(SIGN_IN))
                    .andExpect(jsonPath("$.instance").value(url));
        }
    }

    @Test
    void eachRoleIsKeptInItsOwnArea() throws Exception {
        String customer = customer("ana@example.com");
        String rider = rider("Rider One", "rider1@example.com", true);
        String admin = admin();
        String staff = staff();

        getJson(CUSTOMER_AREA, customer).andExpect(status().isOk());
        getJson(RIDER_AREA, rider).andExpect(status().isOk());
        getJson(ADMIN_AREA, admin).andExpect(status().isOk());
        getJson(ADMIN_AREA, staff).andExpect(status().isOk());

        Map<String, List<String>> refused = Map.of(
                customer, List.of(RIDER_AREA, ADMIN_AREA),
                rider, List.of(CUSTOMER_AREA, ADMIN_AREA, "/api/geocode/reverse?lat=14.6&lng=121"),
                admin, List.of(CUSTOMER_AREA, RIDER_AREA),
                staff, List.of(CUSTOMER_AREA, RIDER_AREA));
        for (Map.Entry<String, List<String>> entry : refused.entrySet()) {
            for (String url : entry.getValue()) {
                getJson(url, entry.getKey())
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.detail").value(NO_ACCESS));
            }
        }
    }

    @Test
    void writeEndpointsAreProtectedToo() throws Exception {
        String customer = customer("ana@example.com");
        String rider = rider("Rider One", "rider1@example.com", true);

        postJson("/api/orders", rider, walkInOrder(8, "Standard")).andExpect(status().isForbidden());
        postJson("/api/rider/jobs/1/accept", customer, null).andExpect(status().isForbidden());
        patchJson("/api/admin/orders/1", rider, Map.of("status", "READY")).andExpect(status().isForbidden());
        putJson("/api/admin/pricing", customer, Map.of()).andExpect(status().isForbidden());
        postJson("/api/rider/jobs/1/accept", null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedOrStaleTokensAre401() throws Exception {
        String customer = customer("ana@example.com");
        String forged = customer.substring(0, customer.length() - 2) + (customer.endsWith("AA") ? "BB" : "AA");

        getJson(CUSTOMER_AREA, forged).andExpect(status().isUnauthorized());
        jdbc.update("DELETE FROM users");
        getJson(CUSTOMER_AREA, customer)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please sign in again."));
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        getJson("/api/ping", null).andExpect(status().isOk());
        getJson("/api/pricing", null).andExpect(status().isOk());
        getJson("/api/shop", null).andExpect(status().isOk());
    }

    // ---- sign in per tab ----------------------------------------------------------------------

    @Test
    void signInChecksTheTabForEveryRole() throws Exception {
        customer("ana@example.com");
        rider("Rider One", "rider1@example.com", true);
        admin();

        postJson("/api/auth/login", null, login("ana@example.com", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.usesAdminConsole").value(false));
        postJson("/api/auth/login", null, login("rider1@example.com", "RIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.onDuty").value(true));
        postJson("/api/auth/login", null, login("owner@laundryhub.test", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.position").value("Owner"));
        // No tab given: any role may sign in (the console login does not send one).
        postJson("/api/auth/login", null, login("owner@laundryhub.test", null)).andExpect(status().isOk());

        postJson("/api/auth/login", null, login("ana@example.com", "RIDER")).andExpect(status().isForbidden());
        postJson("/api/auth/login", null, login("ana@example.com", "ADMIN")).andExpect(status().isForbidden());
        postJson("/api/auth/login", null, login("owner@laundryhub.test", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("This is not a customer account. Choose the right tab."));
        postJson("/api/auth/login", null, login("nobody@example.com", "CUSTOMER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."));
        postJson("/api/auth/login", null, login("ana@example.com", "PILOT")).andExpect(status().isBadRequest());
    }

    @Test
    void tokenFromSignInOpensTheRightArea() throws Exception {
        rider("Rider One", "rider1@example.com", false);

        String token = read(postJson("/api/auth/login", null, login("rider1@example.com", "RIDER")), "$.token");

        getJson(RIDER_AREA, token).andExpect(status().isOk()).andExpect(jsonPath("$.onDuty").value(false));
        getJson(CUSTOMER_AREA, token).andExpect(status().isForbidden());
    }

    // ---- 400 with field errors ----------------------------------------------------------------

    @Test
    void emptyOrderListsEveryMissingField() throws Exception {
        postJson("/api/orders", customer("ana@example.com"), Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Some fields are not valid."))
                .andExpect(jsonPath("$.errors.serviceType").exists())
                .andExpect(jsonPath("$.errors.weightKg").exists())
                .andExpect(jsonPath("$.errors.soapId").exists())
                .andExpect(jsonPath("$.errors.scheduleDate").exists())
                .andExpect(jsonPath("$.errors.slotLabel").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists());
    }

    @Test
    void nestedAndNumericFieldsAreValidated() throws Exception {
        String customer = customer("ana@example.com");
        Map<String, Object> badPin = deliveryOrder(10, "Standard", NEAR_PIN, "CASH");
        badPin.put("pin", Map.of("latitude", 91, "longitude", 121));
        badPin.put("weightKg", -2);

        postJson("/api/orders", customer, badPin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['pin.latitude']").exists())
                .andExpect(jsonPath("$.errors.weightKg").exists());
        postJson("/api/orders/quote", customer, Map.of("serviceType", "WALK_IN", "weightKg", 0, "soapId",
                soapId("Standard")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weightKg").exists());
    }

    @Test
    void riderAndLoginFormsAreValidated() throws Exception {
        String rider = rider("Rider One", "rider1@example.com", true);

        postJson("/api/rider/location", rider, Map.of("lat", 100, "lng", 121))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());
        postJson("/api/rider/jobs/1/advance", rider, Map.of("weighedKg", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weighedKg").exists());
        postJson("/api/auth/login", null, Map.of("email", "", "password", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void adminFormsReturnFieldErrors() throws Exception {
        String admin = admin();

        postJson("/api/admin/users", admin, Map.of("role", "CUSTOMER", "fullName", "", "email", "not-an-email",
                "mobileNumber", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.mobileNumber").value("must look like 09XXXXXXXXX"));
        postJson("/api/admin/soaps", admin, Map.of("name", "Eco", "description", "Plant-based", "addOnPerKg", -1,
                "available", true, "tintHex", "blue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.addOnPerKg").exists())
                .andExpect(jsonPath("$.errors.tintHex").value("must be a color like #E4EEF8"));
    }

    @Test
    void unreadableBodiesAndBadIdsAre400() throws Exception {
        String customer = customer("ana@example.com");

        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        getJson("/api/orders/abc", customer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'id' has an invalid value."));
    }
}
