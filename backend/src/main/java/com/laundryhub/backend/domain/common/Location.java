package com.laundryhub.backend.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A pinned point on the map (screen 04a) with optional address details
 * ("House no., street, barangay, landmark"). Immutable value object.
 */
// OOP: Encapsulation — coordinates are validated once in the constructor and never change.
@Embeddable
public class Location {

    /** Decimal places stored for coordinates (DECIMAL(9,6), about 0.1 m). */
    public static final int COORDINATE_SCALE = 6;

    /** Longest address-details text the schema stores. */
    public static final int MAX_ADDRESS_LENGTH = 255;

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final double EARTH_RADIUS_KM = 6371.0088;

    @Column(name = "latitude", precision = 9, scale = COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = COORDINATE_SCALE)
    private BigDecimal longitude;

    @Column(name = "address", length = MAX_ADDRESS_LENGTH)
    private String address;

    /** For JPA only. */
    protected Location() {
    }

    /**
     * Creates a location.
     *
     * @param latitude  degrees, -90 to 90
     * @param longitude degrees, -180 to 180
     * @param address   address details, may be {@code null}
     * @throws IllegalArgumentException if a coordinate is missing or out of range
     */
    public Location(BigDecimal latitude, BigDecimal longitude, String address) {
        this.latitude = inRange(latitude, MAX_LATITUDE, "Latitude");
        this.longitude = inRange(longitude, MAX_LONGITUDE, "Longitude");
        this.address = Check.optionalText(address, "Address details", MAX_ADDRESS_LENGTH);
    }

    private static BigDecimal inRange(BigDecimal value, BigDecimal max, String field) {
        Check.notNull(value, field);
        if (value.abs().compareTo(max) > 0) {
            throw new IllegalArgumentException(field + " must be between -" + max + " and " + max);
        }
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Great-circle distance, used for the delivery radius and the "1.2 km" on rider cards.
     *
     * @param other the other point
     * @return distance in kilometres
     */
    public double distanceKmTo(Location other) {
        Check.notNull(other, "Other location");
        double lat1 = Math.toRadians(latitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(other.longitude.doubleValue() - longitude.doubleValue());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    /** @return latitude in degrees */
    public BigDecimal getLatitude() {
        return latitude;
    }

    /** @return longitude in degrees */
    public BigDecimal getLongitude() {
        return longitude;
    }

    /** @return address details, or {@code null} */
    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location other)) {
            return false;
        }
        return latitude.compareTo(other.latitude) == 0
                && longitude.compareTo(other.longitude) == 0
                && Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude.stripTrailingZeros(), longitude.stripTrailingZeros(), address);
    }

    @Override
    public String toString() {
        return latitude + ", " + longitude + (address == null ? "" : " (" + address + ")");
    }
}
