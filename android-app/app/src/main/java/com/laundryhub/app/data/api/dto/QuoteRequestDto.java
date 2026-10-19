package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** Body of {@code POST /api/orders/quote}. */
public final class QuoteRequestDto {

    // OOP: Encapsulation — an immutable request.
    private final String serviceType;
    private final BigDecimal weightKg;
    private final long soapId;

    /**
     * @param serviceType WALK_IN or PICKUP_DELIVERY
     * @param weightKg    kilos
     * @param soapId      soap
     */
    public QuoteRequestDto(String serviceType, BigDecimal weightKg, long soapId) {
        this.serviceType = serviceType;
        this.weightKg = weightKg;
        this.soapId = soapId;
    }

    /** @return the service sent */
    public String getServiceType() {
        return serviceType;
    }
}
