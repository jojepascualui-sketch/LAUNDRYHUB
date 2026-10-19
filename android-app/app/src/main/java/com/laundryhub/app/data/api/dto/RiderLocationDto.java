package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** Body of {@code POST /api/rider/location}. */
public final class RiderLocationDto {

    // OOP: Encapsulation — an immutable request.
    private final BigDecimal lat;
    private final BigDecimal lng;
    private final BigDecimal accuracyMeters;

    /**
     * @param lat            latitude
     * @param lng            longitude
     * @param accuracyMeters GPS accuracy, or {@code null}
     */
    public RiderLocationDto(BigDecimal lat, BigDecimal lng, BigDecimal accuracyMeters) {
        this.lat = lat;
        this.lng = lng;
        this.accuracyMeters = accuracyMeters;
    }
}
