package com.laundryhub.app.model;

import androidx.annotation.Nullable;

import java.math.BigDecimal;

/**
 * The server's answer for a pin ({@code GET /api/geocode/reverse}): the address Google found and
 * whether the pin is inside the admin's delivery radius (Haversine distance from the shop).
 *
 * @param address    address for the pin, or {@code null} if none was found
 * @param distanceKm distance from the shop, or {@code null} if the shop position is not set
 * @param radiusKm   delivery radius
 * @param inside     whether an order with this pin will be accepted
 */
public record PinCheck(@Nullable String address, @Nullable BigDecimal distanceKm, @Nullable BigDecimal radiusKm,
                       boolean inside) {

    /** @return whether the server could judge the distance (the shop position is set) */
    public boolean isKnown() {
        return distanceKm != null && radiusKm != null;
    }
}
