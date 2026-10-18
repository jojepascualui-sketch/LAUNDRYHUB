package com.laundryhub.backend.domain.pricing;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.order.ServiceCode;

/**
 * The itemized price shown on "Order summary" (screen 05) and the A2 detail panel:
 * Laundry (kg × rate), soap (kg × add-on), Pickup &amp; delivery fee, and Total.
 *
 * @param serviceCode    chosen service
 * @param serviceName    service name, e.g. "Pickup &amp; Delivery"
 * @param billableKg     kilos charged (never below the minimum load)
 * @param ratePerKg      price per kilo
 * @param laundryCost    billableKg × ratePerKg
 * @param soapName       chosen soap
 * @param soapAddOnPerKg soap price per kilo
 * @param soapCost       billableKg × soapAddOnPerKg
 * @param serviceFee     delivery fee, or 0 for walk-in
 * @param total          laundryCost + soapCost + serviceFee
 */
public record PriceBreakdown(
        ServiceCode serviceCode,
        String serviceName,
        BigDecimal billableKg,
        BigDecimal ratePerKg,
        BigDecimal laundryCost,
        String soapName,
        BigDecimal soapAddOnPerKg,
        BigDecimal soapCost,
        BigDecimal serviceFee,
        BigDecimal total) {

    /**
     * Validates that every line is present and the lines add up to the total.
     *
     * @throws IllegalArgumentException if a value is missing or the total does not add up
     */
    public PriceBreakdown {
        Check.notNull(serviceCode, "Service");
        Check.notNull(serviceName, "Service name");
        Check.notNull(soapName, "Soap name");
        Check.positive(billableKg, "Billable kilos");
        Check.positive(ratePerKg, "Price per kilo");
        Check.nonNegative(soapAddOnPerKg, "Soap add-on");
        BigDecimal sum = Check.nonNegative(laundryCost, "Laundry cost")
                .add(Check.nonNegative(soapCost, "Soap cost"))
                .add(Check.nonNegative(serviceFee, "Service fee"));
        if (sum.compareTo(Check.notNull(total, "Total")) != 0) {
            throw new IllegalArgumentException("Total " + total + " does not equal the lines " + sum);
        }
    }
}
