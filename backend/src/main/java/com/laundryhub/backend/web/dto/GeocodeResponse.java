package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

/**
 * 04a feedback after the customer drops the pin.
 *
 * @param latitude           pin latitude
 * @param longitude          pin longitude
 * @param address            address Google found, or {@code null}
 * @param distanceKm         distance from the shop, or {@code null} if the shop position is not set
 * @param deliveryRadiusKm   admin's radius
 * @param insideDeliveryArea whether an order with this pin will be accepted
 */
public record GeocodeResponse(BigDecimal latitude, BigDecimal longitude, String address, BigDecimal distanceKm,
                              BigDecimal deliveryRadiusKm, boolean insideDeliveryArea) {
}
