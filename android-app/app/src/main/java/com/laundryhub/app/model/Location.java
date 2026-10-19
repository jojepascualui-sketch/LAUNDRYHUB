package com.laundryhub.app.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A pinned point on the map (04a): latitude and longitude, validated and kept to six decimals
 * (about 0.1 m), the same precision the backend stores. Immutable. Sent with the order.
 */
public final class Location {

    /** Decimal places kept (DECIMAL(9,6) on the server). */
    public static final int SCALE = 6;

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    // OOP: Encapsulation — validated once in the constructor, never changed.
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    /**
     * @param latitude  degrees, -90 to 90
     * @param longitude degrees, -180 to 180
     * @throws IllegalArgumentException if a value is missing or out of range
     */
    public Location(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = inRange(latitude, MAX_LATITUDE, "Latitude");
        this.longitude = inRange(longitude, MAX_LONGITUDE, "Longitude");
    }

    /**
     * From map or GPS coordinates.
     *
     * @param latitude  degrees
     * @param longitude degrees
     * @return the location
     * @throws IllegalArgumentException if a value is not a finite number in range
     */
    @NonNull
    public static Location of(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new IllegalArgumentException("Coordinates must be finite numbers");
        }
        return new Location(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    }

    private static BigDecimal inRange(BigDecimal value, BigDecimal max, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.abs().compareTo(max) > 0) {
            throw new IllegalArgumentException(field + " must be between -" + max + " and " + max);
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** @return latitude in degrees */
    @NonNull
    public BigDecimal getLatitude() {
        return latitude;
    }

    /** @return longitude in degrees */
    @NonNull
    public BigDecimal getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Location other && latitude.equals(other.latitude) && longitude.equals(other.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @NonNull
    @Override
    public String toString() {
        return latitude.toPlainString() + "," + longitude.toPlainString();
    }
}
