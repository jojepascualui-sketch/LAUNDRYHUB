package com.laundryhub.backend.service.geo;

import java.math.BigDecimal;
import java.util.Optional;

/** Turns a pinned point into a street address (reverse geocoding). */
// OOP: Abstraction — order code does not know Google is behind it; tests use a fake.
public interface GeocodingService {

    /**
     * @param latitude  degrees
     * @param longitude degrees
     * @return the formatted address, or empty if none was found or the service is off or failing
     */
    Optional<String> reverseGeocode(BigDecimal latitude, BigDecimal longitude);
}
