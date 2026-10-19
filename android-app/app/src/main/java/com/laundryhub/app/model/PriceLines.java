package com.laundryhub.app.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

/**
 * "Order summary" lines on 05 and the footer total: Laundry (kg × rate), soap (kg × add-on),
 * Pickup &amp; delivery fee, Total.
 *
 * @param billableKg     kilos charged
 * @param ratePerKg      price per kilo
 * @param laundryCost    kilos × rate
 * @param soapName       soap
 * @param soapAddOnPerKg soap price per kilo
 * @param soapCost       kilos × add-on
 * @param deliveryFee    fee (0 for walk-in)
 * @param total          the total
 * @param fromServer     whether the server calculated it ({@code POST /api/orders/quote})
 */
public record PriceLines(@NonNull BigDecimal billableKg, @NonNull BigDecimal ratePerKg, @NonNull BigDecimal laundryCost,
                         @NonNull String soapName, @NonNull BigDecimal soapAddOnPerKg, @NonNull BigDecimal soapCost,
                         @NonNull BigDecimal deliveryFee, @NonNull BigDecimal total, boolean fromServer) {

    /** Checks that the lines add up. */
    public PriceLines {
        if (laundryCost.add(soapCost).add(deliveryFee).compareTo(total) != 0) {
            throw new IllegalArgumentException("Price lines do not add up to the total");
        }
    }
}
