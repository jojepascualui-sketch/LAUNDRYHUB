package com.laundryhub.admin.api.dto;

/**
 * Customer pin (delivery orders).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record PinnedLocation(java.math.BigDecimal latitude, java.math.BigDecimal longitude, String addressDetails, String geocodedAddress, String mapsUrl) {
}
