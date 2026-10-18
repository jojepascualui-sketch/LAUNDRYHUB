package com.laundryhub.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Customer screens 01–07: prices, quote, ordering rules, Orders tab and tracking. */
class CustomerOrderApiTest extends ApiTestBase {

    @Test
    void pricingIsPublicAndMatchesTheDesignDefaults() throws Exception {
        getJson("/api/pricing", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerKg").value(30.00))
                .andExpect(jsonPath("$.minKg").value(8.00))
                .andExpect(jsonPath("$.deliveryFee").value(60.00))
                .andExpect(jsonPath("$.services[1].code").value("PICKUP_DELIVERY"))
                .andExpect(jsonPath("$.services[1].fee").value(60.00))
                .andExpect(jsonPath("$.services[0].fee").value(0))
                .andExpect(jsonPath("$.soaps", hasSize(4)))
                .andExpect(jsonPath("$.soaps[0].name").value("Standard"))
                .andExpect(jsonPath("$.soaps[0].included").value(true));
        getJson("/api/shop", null).andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value(14.6));
    }

    @Test
    void timeChipsMatchTheDesign() throws Exception {
        String customer = customer("ana@example.com");
        getJson("/api/orders/slots?service=PICKUP_DELIVERY", customer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].text", contains("Today, 2–4 PM", "Today, 4–6 PM",
                        "Tomorrow, 9–11 AM", "Tomorrow, 1–3 PM")));
        getJson("/api/orders/slots?service=WALK_IN", customer)
                .andExpect(jsonPath("$[*].text", contains("Today, morning", "Today, afternoon",
                        "Tomorrow, morning", "Tomorrow, afternoon")));
    }

    @Test
    void quoteUsesTheServerFormula() throws Exception {
        String customer = customer("ana@example.com");
        // CLAUDE.md example: 10 kg, Hypoallergenic, Pickup & Delivery = 300 + 50 + 60 = 410.
        postJson("/api/orders/quote", customer,
                Map.of("serviceType", "PICKUP_DELIVERY", "weightKg", 10, "soapId", soapId("Hypoallergenic")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laundryCost").value(300.00))
                .andExpect(jsonPath("$.soapCost").value(50.00))
                .andExpect(jsonPath("$.deliveryFee").value(60.00))
                .andExpect(jsonPath("$.total").value(410.00));
        postJson("/api/orders/quote", customer,
                Map.of("serviceType", "WALK_IN", "weightKg", 8, "soapId", soapId("Standard")))
                .andExpect(jsonPath("$.total").value(240.00));
    }

    @Test
    void belowTheMinimumLoadIsRejected() throws Exception {
        String customer = customer("ana@example.com");
        postJson("/api/orders/quote", customer,
                Map.of("serviceType", "WALK_IN", "weightKg", 7.5, "soapId", soapId("Standard")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Minimum of 8 kg per order"));
        postJson("/api/orders", customer, walkInOrder(7, "Standard")).andExpect(status().isBadRequest());
    }

    @Test
    void deliveryNeedsAPinInsideTheRadius() throws Exception {
        String customer = customer("ana@example.com");

        postJson("/api/orders", customer, deliveryOrder(10, "Standard", null, "CASH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Pin your location")));
        postJson("/api/orders", customer, deliveryOrder(10, "Standard", FAR_PIN, "CASH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("We deliver within 5 km")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
    }

    @Test
    void placingADeliveryOrderSavesTheServerPriceAndTwoJobs() throws Exception {
        String customer = customer("ana@example.com");
        when(geocoding.reverseGeocode(any(), any())).thenReturn(Optional.of("Rizal St, Poblacion, Makati"));
        Map<String, Object> body = deliveryOrder(10, "Hypoallergenic", NEAR_PIN, "GCASH");
        body.put("total", 1.00); // a client price is ignored

        postJson("/api/orders", customer, body)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/orders/")))
                .andExpect(jsonPath("$.orderNo").value("LH-0001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price.total").value(410.00))
                .andExpect(jsonPath("$.payment.method").value("GCASH"))
                .andExpect(jsonPath("$.payment.instructions").value("Pay instantly from your e-wallet"))
                .andExpect(jsonPath("$.schedule.text").value("Today, 2–4 PM"))
                .andExpect(jsonPath("$.pickupLocation.addressDetails").value(containsString("Rizal St.")))
                .andExpect(jsonPath("$.pickupLocation.geocodedAddress").value("Rizal St, Poblacion, Makati"));

        assertThat(jdbc.queryForList("SELECT job_type, status FROM rider_jobs ORDER BY job_type"))
                .extracting(row -> row.get("job_type") + "/" + row.get("status"))
                .containsExactly("PICKUP/AVAILABLE", "RETURN_DELIVERY/WAITING");
        assertThat(jdbc.queryForObject("SELECT status FROM order_status_history", String.class)).isEqualTo("PENDING");

        postJson("/api/orders", customer, walkInOrder(9, "Baby-Safe"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNo").value("LH-0002"))
                .andExpect(jsonPath("$.price.deliveryFee").value(0))
                .andExpect(jsonPath("$.price.total").value(324.00))
                .andExpect(jsonPath("$.payment.instructions").value("Pay at the counter"))
                .andExpect(jsonPath("$.pickupLocation").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rider_jobs", Integer.class)).isEqualTo(2);
    }

    @Test
    void orderFormIsValidated() throws Exception {
        String customer = customer("ana@example.com");
        Map<String, Object> gcashWithoutNumber = deliveryOrder(10, "Standard", NEAR_PIN, "GCASH");
        gcashWithoutNumber.remove("gcashNumber");
        postJson("/api/orders", customer, gcashWithoutNumber)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("GCash mobile number")));

        Map<String, Object> wrongChip = walkInOrder(8, "Standard");
        wrongChip.put("slotLabel", "2–4 PM"); // a delivery chip on a walk-in order
        postJson("/api/orders", customer, wrongChip).andExpect(status().isBadRequest());

        Map<String, Object> badEnum = walkInOrder(8, "Standard");
        badEnum.put("paymentMethod", "BITCOIN");
        postJson("/api/orders", customer, badEnum).andExpect(status().isBadRequest());

        jdbc.update("UPDATE pricing_settings SET gcash_enabled = FALSE");
        postJson("/api/orders", customer, deliveryOrder(10, "Standard", NEAR_PIN, "GCASH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("GCash is not available right now"));
    }

    @Test
    void customersOnlySeeTheirOwnOrders() throws Exception {
        String ana = customer("ana@example.com");
        String juan = customer("juan@example.com");
        long id = placeOrder(ana, walkInOrder(8, "Standard"));

        getJson("/api/orders/mine", ana).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scheduleText").value("Tomorrow, morning"));
        getJson("/api/orders/mine", juan).andExpect(jsonPath("$", hasSize(0)));
        getJson("/api/orders/" + id, ana).andExpect(status().isOk());
        getJson("/api/orders/" + id, juan).andExpect(status().isNotFound());
        getJson("/api/orders/" + id + "/tracking", juan).andExpect(status().isNotFound());
    }

    @Test
    void newOrderTrackingStartsAtOrderPlaced() throws Exception {
        String customer = customer("ana@example.com");
        long id = placeOrder(customer, deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));

        getJson("/api/orders/" + id + "/tracking", customer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(0))
                .andExpect(jsonPath("$.timeline", hasSize(5)))
                .andExpect(jsonPath("$.timeline[0].current").value(true))
                .andExpect(jsonPath("$.rider").doesNotExist())
                .andExpect(jsonPath("$.liveTracking").value(false))
                .andExpect(jsonPath("$.shop.latitude").value(14.6))
                .andExpect(jsonPath("$.pin.latitude").value(NEAR_PIN[0]));
    }

    @Test
    void pinCheckReportsAddressAndRadius() throws Exception {
        String customer = customer("ana@example.com");
        when(geocoding.reverseGeocode(any(), any())).thenReturn(Optional.of("Poblacion, Makati"));

        getJson("/api/geocode/reverse?lat=14.61&lng=121.01", customer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Poblacion, Makati"))
                .andExpect(jsonPath("$.distanceKm").value(1.5))
                .andExpect(jsonPath("$.insideDeliveryArea").value(true));
        getJson("/api/geocode/reverse?lat=14.7&lng=121.1", customer)
                .andExpect(jsonPath("$.insideDeliveryArea").value(false));
        getJson("/api/geocode/reverse?lat=95&lng=121.1", customer).andExpect(status().isBadRequest());
    }
}
