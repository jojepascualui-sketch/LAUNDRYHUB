package com.laundryhub.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** A2 map page and the health endpoints answer without a token. */
class MapPageApiTest extends ApiTestBase {

    @Test
    void mapPageNeedsNoTokenAndShowsThePlainFrameWithoutAKey() throws Exception {
        mvc.perform(get("/api/maps/order-pin").param("lat", "14.6091").param("lng", "121.0223"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(Matchers.containsString("MAPS_JS_KEY")));
    }

    @Test
    void outOfRangeCoordinatesAre400() throws Exception {
        mvc.perform(get("/api/maps/order-pin").param("lat", "95").param("lng", "121"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }
}
