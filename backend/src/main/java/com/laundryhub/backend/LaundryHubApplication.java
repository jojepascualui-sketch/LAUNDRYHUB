package com.laundryhub.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Laundry HUB REST API: every price and business rule lives here. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LaundryHubApplication {

    /** Time zone of the shop; "today" on the dashboard, slots and rider counters uses it. */
    public static final String DEFAULT_SHOP_TIME_ZONE = "Asia/Manila";

    /**
     * Starts the server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        String zone = System.getenv().getOrDefault("SHOP_TIME_ZONE", DEFAULT_SHOP_TIME_ZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(LaundryHubApplication.class, args);
    }
}
