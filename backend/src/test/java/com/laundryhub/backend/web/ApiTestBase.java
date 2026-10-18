package com.laundryhub.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Staff;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.repository.UserRepository;
import com.laundryhub.backend.security.GoogleTokenVerifier;
import com.laundryhub.backend.security.TokenService;
import com.laundryhub.backend.service.geo.GeocodingService;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Shared setup for the MockMvc integration tests: full Spring context, real security and
 * Flyway schema on H2, Google sign-in and geocoding replaced by mocks, and a clean database
 * before every test. Shop is at 14.6, 121.0 (application-test.yml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    /** Password of every account created by the helpers. */
    protected static final String PASSWORD = "secret-pass-1";

    /** A pin about 1.5 km from the test shop. */
    protected static final double[] NEAR_PIN = {14.610000, 121.010000};

    /** A pin about 3 km from the test shop. */
    protected static final double[] FARTHER_PIN = {14.620000, 121.020000};

    /** A pin about 15 km from the test shop, outside the 5 km radius. */
    protected static final double[] FAR_PIN = {14.700000, 121.100000};

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected SoapRepository soaps;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TokenService tokens;

    @MockitoBean
    protected GeocodingService geocoding;

    @MockitoBean
    protected GoogleTokenVerifier googleVerifier;

    /** Empties every table the tests write to and restores the A3 defaults. */
    @BeforeEach
    void cleanDatabase() {
        for (String table : new String[] {"rider_locations", "rider_job_declines", "rider_jobs", "payments",
                "order_status_history", "orders", "riders"}) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("UPDATE pricing_settings SET rate_per_kg = 30.00, min_kg = 8.00, delivery_fee = 60.00, "
                + "delivery_radius_km = 5.00, cash_enabled = TRUE, gcash_enabled = TRUE, shop_gcash_number = NULL, "
                + "updated_by = NULL WHERE id = 1");
        jdbc.update("UPDATE shop_settings SET updated_by = NULL WHERE id = 1");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM soaps WHERE name NOT IN ('Standard', 'Hypoallergenic', 'Premium Scented', 'Baby-Safe')");
        jdbc.update("UPDATE soaps SET available = TRUE");
    }

    // ---- accounts -----------------------------------------------------------------------------

    /**
     * @param email e-mail
     * @return bearer token of a new customer
     */
    protected String customer(String email) {
        return save(new Customer("Ana Reyes", email, "09170000001"));
    }

    /**
     * @param name   full name
     * @param email  e-mail
     * @param onDuty duty switch
     * @return bearer token of a new rider
     */
    protected String rider(String name, String email, boolean onDuty) {
        Rider rider = new Rider(name, email, "09180000002", "SMP " + Math.abs(email.hashCode() % 10000));
        if (onDuty) {
            rider.goOnDuty();
        }
        return save(rider);
    }

    /** @return bearer token of a new admin */
    protected String admin() {
        return save(new Admin("Admin Owner", "owner@laundryhub.test", Admin.DEFAULT_POSITION));
    }

    /** @return bearer token of a new staff member */
    protected String staff() {
        return save(new Staff("Counter Staff", "counter@laundryhub.test", "Counter staff"));
    }

    private String save(User user) {
        user.changePasswordHash(passwordEncoder.encode(PASSWORD));
        return tokens.issue(users.save(user)).value();
    }

    /**
     * @param token a bearer token
     * @return the user id in it
     */
    protected Long idOf(String token) throws java.text.ParseException {
        return Long.valueOf(com.nimbusds.jwt.SignedJWT.parse(token).getJWTClaimsSet().getSubject());
    }

    /**
     * @param name soap name
     * @return its id
     */
    protected Long soapId(String name) {
        return soaps.findAll().stream().filter(s -> s.getName().equals(name)).map(Soap::getId).findFirst()
                .orElseThrow();
    }

    // ---- requests -----------------------------------------------------------------------------

    /**
     * @param builder request
     * @param token   bearer token, or {@code null}
     * @param body    JSON body object, or {@code null}
     * @return the result
     * @throws Exception on MockMvc errors
     */
    protected ResultActions call(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(builder);
    }

    protected ResultActions getJson(String url, String token) throws Exception {
        return call(get(url), token, null);
    }

    protected ResultActions postJson(String url, String token, Object body) throws Exception {
        return call(post(url), token, body);
    }

    protected ResultActions patchJson(String url, String token, Object body) throws Exception {
        return call(patch(url), token, body);
    }

    protected ResultActions putJson(String url, String token, Object body) throws Exception {
        return call(put(url), token, body);
    }

    /**
     * @param result a finished request
     * @param path   JsonPath expression
     * @param <T>    value type
     * @return the value at the path
     * @throws Exception if the body cannot be read
     */
    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    // ---- orders -------------------------------------------------------------------------------

    /**
     * A Pickup &amp; Delivery order body: "Today, 2–4 PM".
     *
     * @param kg      kilos
     * @param soap    soap name
     * @param pin     lat/lng
     * @param payment CASH or GCASH
     * @return the body
     */
    protected Map<String, Object> deliveryOrder(double kg, String soap, double[] pin, String payment) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("serviceType", "PICKUP_DELIVERY");
        body.put("weightKg", kg);
        body.put("soapId", soapId(soap));
        body.put("scheduleDate", LocalDate.now().toString());
        body.put("slotLabel", "2–4 PM");
        if (pin != null) {
            body.put("pin", Map.of("latitude", pin[0], "longitude", pin[1]));
        }
        body.put("addressDetails", "Blk 1 Lot 2, Rizal St., Brgy. Poblacion");
        body.put("paymentMethod", payment);
        if ("GCASH".equals(payment)) {
            body.put("gcashNumber", "09170001021");
        }
        return body;
    }

    /**
     * A Walk-in order body: "Tomorrow, morning", Cash.
     *
     * @param kg   kilos
     * @param soap soap name
     * @return the body
     */
    protected Map<String, Object> walkInOrder(double kg, String soap) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("serviceType", "WALK_IN");
        body.put("weightKg", kg);
        body.put("soapId", soapId(soap));
        body.put("scheduleDate", LocalDate.now().plusDays(1).toString());
        body.put("slotLabel", "morning");
        body.put("paymentMethod", "CASH");
        return body;
    }

    /**
     * Places an order and returns its id.
     *
     * @param customerToken customer
     * @param body          order body
     * @return order id
     * @throws Exception on request errors
     */
    protected long placeOrder(String customerToken, Map<String, Object> body) throws Exception {
        Number id = read(postJson("/api/orders", customerToken, body), "$.id");
        return id.longValue();
    }

    /**
     * @param orderId order
     * @param type    PICKUP or RETURN_DELIVERY
     * @return the job id
     */
    protected long jobId(long orderId, String type) {
        return jdbc.queryForObject("SELECT id FROM rider_jobs WHERE order_id = ? AND job_type = ?", Long.class,
                orderId, type);
    }

    /**
     * @param orderId order
     * @return the order status column
     */
    protected String orderStatus(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }
}
