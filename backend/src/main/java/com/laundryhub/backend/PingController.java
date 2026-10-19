package com.laundryhub.backend;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health check for clients and setup: {@code GET /api/ping} answers without signing in. */
@RestController
@RequestMapping("/api")
public class PingController {

    private static final String STATUS_KEY = "status";
    private static final String STATUS_OK = "ok";
    private static final String SERVICE_KEY = "service";
    private static final String SERVICE_NAME = "laundryhub-backend";

    /** @return {@code {"status":"ok","service":"laundryhub-backend"}} */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(STATUS_KEY, STATUS_OK, SERVICE_KEY, SERVICE_NAME);
    }
}
