package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.pricing.PriceBreakdown;

/**
 * "Order summary" on 05 and the price block on A2: Laundry (kg × rate), soap (kg × add-on),
 * Pickup &amp; delivery fee, Total. Calculated by the server.
 *
 * @param serviceType    service code
 * @param serviceName    e.g. "Pickup &amp; Delivery"
 * @param billableKg     kilos charged (never below the minimum)
 * @param ratePerKg      price per kilo
 * @param laundryCost    billableKg × ratePerKg
 * @param soapName       soap
 * @param soapAddOnPerKg soap price per kilo
 * @param soapCost       billableKg × soapAddOnPerKg
 * @param deliveryFee    delivery fee, 0 for walk-in
 * @param total          the amount charged
 */
public record PriceBreakdownResponse(ServiceCode serviceType, String serviceName, BigDecimal billableKg,
                                     BigDecimal ratePerKg, BigDecimal laundryCost, String soapName,
                                     BigDecimal soapAddOnPerKg, BigDecimal soapCost, BigDecimal deliveryFee,
                                     BigDecimal total) {

    /**
     * @param price a domain breakdown
     * @return the DTO
     */
    public static PriceBreakdownResponse from(PriceBreakdown price) {
        return new PriceBreakdownResponse(price.serviceCode(), price.serviceName(), price.billableKg(),
                price.ratePerKg(), price.laundryCost(), price.soapName(), price.soapAddOnPerKg(), price.soapCost(),
                price.serviceFee(), price.total());
    }
}
