package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** {@code GET /api/geocode/reverse}: address and delivery-radius check for a pin. Filled by Gson. */
public final class GeocodeDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private String address;
    private BigDecimal distanceKm;
    private BigDecimal deliveryRadiusKm;
    private boolean insideDeliveryArea;

    /** For Gson. */
    public GeocodeDto() {
    }

    /** @return address Google found, or {@code null} */
    public String getAddress() {
        return address;
    }

    /** @return distance from the shop, or {@code null} if the shop position is not set */
    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    /** @return the admin's delivery radius */
    public BigDecimal getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    /** @return whether an order with this pin will be accepted */
    public boolean isInsideDeliveryArea() {
        return insideDeliveryArea;
    }
}
