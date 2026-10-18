package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.common.Location;

/**
 * A pinned point with its addresses, as on A2 "Pinned location" (address · coordinates · "Open in Google Maps").
 *
 * @param latitude        degrees
 * @param longitude       degrees
 * @param addressDetails  what the customer typed, or {@code null}
 * @param geocodedAddress what Google found for the point, or {@code null}
 * @param mapsUrl         link that opens the point in Google Maps
 */
public record LocationResponse(BigDecimal latitude, BigDecimal longitude, String addressDetails,
                               String geocodedAddress, String mapsUrl) {

    /** Google Maps search link; the point is appended as "lat,lng". */
    public static final String MAPS_SEARCH_URL = "https://www.google.com/maps/search/?api=1&query=";

    /** Google Maps directions link (R2/R3 "Navigate in Google Maps"). */
    public static final String MAPS_DIRECTIONS_URL = "https://www.google.com/maps/dir/?api=1&destination=";

    /**
     * @param location        a location, may be {@code null}
     * @param geocodedAddress Google's address, may be {@code null}
     * @return the DTO, or {@code null} if there is no location
     */
    public static LocationResponse from(Location location, String geocodedAddress) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(location.getLatitude(), location.getLongitude(), location.getAddress(),
                geocodedAddress, MAPS_SEARCH_URL + coordinates(location));
    }

    /**
     * @param location a point
     * @return "lat,lng" as used in Google Maps links
     */
    public static String coordinates(Location location) {
        return location.getLatitude().toPlainString() + "," + location.getLongitude().toPlainString();
    }
}
