package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

/**
 * The shop card on 04b ("Drop off at Laundry HUB", address, "Open [SHOP HOURS]") and the shop marker on 07.
 *
 * @param name          shop name
 * @param address       street address
 * @param hoursLabel    opening hours
 * @param latitude      shop latitude, or {@code null} if not set
 * @param longitude     shop longitude, or {@code null} if not set
 * @param contactNumber shop number, or {@code null}
 */
public record ShopResponse(String name, String address, String hoursLabel, BigDecimal latitude,
                           BigDecimal longitude, String contactNumber) {
}
