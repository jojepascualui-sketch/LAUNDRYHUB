package com.laundryhub.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code laundryhub.*} in application.yml. Every secret in here comes from an
 * environment variable and is never logged.
 *
 * @param security  JWT and Google sign-in settings
 * @param geocoding reverse-geocoding settings
 * @param shop      shop position for the delivery radius
 */
@ConfigurationProperties(prefix = "laundryhub")
public record LaundryHubProperties(Security security, Geocoding geocoding, Shop shop) {

    /** Hours a token stays valid when nothing is configured. */
    public static final int DEFAULT_TOKEN_HOURS = 12;

    /**
     * Fills in empty sections so callers never see {@code null}.
     */
    public LaundryHubProperties {
        security = security == null ? new Security(null, DEFAULT_TOKEN_HOURS, null) : security;
        geocoding = geocoding == null ? new Geocoding(null) : geocoding;
        shop = shop == null ? new Shop(null, null) : shop;
    }

    /**
     * @param jwtSecret      HS256 signing key ({@code LAUNDRYHUB_JWT_SECRET}); blank = random per start
     * @param tokenHours     how long a token is valid
     * @param googleClientId OAuth client id the Android app signs in with ({@code GOOGLE_OAUTH_CLIENT_ID})
     */
    public record Security(String jwtSecret, int tokenHours, String googleClientId) {

        /** Replaces a missing or invalid token lifetime with the default. */
        public Security {
            tokenHours = tokenHours > 0 ? tokenHours : DEFAULT_TOKEN_HOURS;
        }
    }

    /**
     * @param apiKey Google server key with the Geocoding API enabled ({@code GOOGLE_SERVER_KEY})
     */
    public record Geocoding(String apiKey) {

        /** @return whether a key is configured */
        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * @param lat shop latitude ({@code SHOP_LAT})
     * @param lng shop longitude ({@code SHOP_LNG})
     */
    public record Shop(BigDecimal lat, BigDecimal lng) {

        /** @return whether both coordinates are set */
        public boolean hasPosition() {
            return lat != null && lng != null;
        }
    }
}
