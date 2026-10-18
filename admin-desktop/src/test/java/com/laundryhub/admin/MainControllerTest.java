package com.laundryhub.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainControllerTest {

    @Test
    void apiBaseUrlPointsAtLocalBackend() {
        assertTrue(MainController.API_BASE_URL.startsWith("http://localhost:8080/"));
    }
}
