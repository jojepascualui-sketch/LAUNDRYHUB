package com.laundryhub.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Where the console finds the backend: environment, then the installer's property, then localhost. */
class AppConfigTest {

    @Test
    void defaultsToTheLocalBackend() {
        AppConfig config = AppConfig.from(Map.of(), Map.of());

        assertEquals("http://localhost:8080/api/", config.apiBaseUrl());
    }

    @Test
    void installerPropertyIsUsedWhenNoVariableIsSet() {
        AppConfig config = AppConfig.from(Map.of(), Map.of("laundryhub.apiUrl", "https://api.example.com/api"));

        assertEquals("https://api.example.com/api/", config.apiBaseUrl());
    }

    @Test
    void environmentWinsOverTheInstaller() {
        AppConfig config = AppConfig.from(Map.of("LAUNDRYHUB_API_URL", "http://shop.local:9000/api/"),
                Map.of("laundryhub.apiUrl", "https://api.example.com/api/"));

        assertEquals("http://shop.local:9000/api/", config.apiBaseUrl());
    }

    @Test
    void mapPageIsServedByTheBackend() {
        AppConfig config = new AppConfig("https://api.example.com/api/");

        assertEquals("https://api.example.com/api/maps/order-pin?lat=14.609100&lng=121.022300",
                config.mapPageUrl("14.609100", "121.022300"));
    }

    @Test
    void rejectsAUrlWithoutSlash() {
        assertThrows(IllegalArgumentException.class, () -> new AppConfig("http://x/api"));
    }
}
