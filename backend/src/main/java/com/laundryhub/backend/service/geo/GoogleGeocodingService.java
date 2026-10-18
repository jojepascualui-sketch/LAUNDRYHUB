package com.laundryhub.backend.service.geo;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.config.LaundryHubProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reverse geocoding with the Google Geocoding API, using the server key in
 * {@code GOOGLE_SERVER_KEY}. Without a key it returns nothing, and orders are still accepted.
 */
// OOP: Inheritance — implements the GeocodingService contract.
@Service
public class GoogleGeocodingService implements GeocodingService {

    /** Geocoding API base URL. */
    public static final String BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final String STATUS_OK = "OK";
    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeocodingService.class);

    // OOP: Encapsulation — the key never leaves this class and is never logged.
    private final String apiKey;
    private final RestClient client;

    /**
     * @param properties holds the server key
     * @param builder    Spring's RestClient builder
     */
    public GoogleGeocodingService(LaundryHubProperties properties, RestClient.Builder builder) {
        this.apiKey = properties.geocoding().apiKey();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        this.client = builder.baseUrl(BASE_URL).requestFactory(factory).build();
    }

    // OOP: Polymorphism — the Google implementation of reverseGeocode.
    @Override
    public Optional<String> reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (apiKey == null || apiKey.isBlank() || latitude == null || longitude == null) {
            return Optional.empty();
        }
        try {
            GeocodeResponse response = client.get()
                    .uri(uri -> uri.queryParam("latlng", latitude.toPlainString() + "," + longitude.toPlainString())
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GeocodeResponse.class);
            if (response == null || !STATUS_OK.equals(response.status()) || response.results() == null
                    || response.results().isEmpty()) {
                LOG.info("Reverse geocoding returned no address (status {})", response == null ? null : response.status());
                return Optional.empty();
            }
            return Optional.ofNullable(response.results().get(0).formatted_address());
        } catch (RestClientException e) {
            // The request URL contains the key, so only the exception type is logged.
            LOG.warn("Reverse geocoding failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** The parts of the Geocoding API response we read. */
    private record GeocodeResponse(String status, List<Result> results) {
    }

    /** One geocoding result; the field name matches Google's JSON. */
    @SuppressWarnings("checkstyle:MemberName")
    private record Result(String formatted_address) {
    }
}
