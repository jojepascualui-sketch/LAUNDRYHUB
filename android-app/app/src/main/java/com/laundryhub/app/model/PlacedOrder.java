package com.laundryhub.app.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

/**
 * What 06 "Order placed!" shows, from the server's response.
 *
 * @param orderNo      "LH-0001"
 * @param serviceName  "Pickup &amp; Delivery"
 * @param weightKg     kilos
 * @param soapName     "Hypoallergenic"
 * @param scheduleText "Today, 2–4 PM"
 * @param paymentName  "GCash"
 * @param total        server total
 */
public record PlacedOrder(@NonNull String orderNo, @NonNull String serviceName, @NonNull BigDecimal weightKg,
                          @NonNull String soapName, @NonNull String scheduleText, @NonNull String paymentName,
                          @NonNull BigDecimal total) {
}
