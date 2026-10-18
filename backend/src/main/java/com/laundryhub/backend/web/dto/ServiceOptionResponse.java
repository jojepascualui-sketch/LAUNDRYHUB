package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;

/**
 * A service card on 02: "Walk-in · No extra fee" or "Pickup &amp; Delivery · + ₱60 delivery fee".
 *
 * @param code        WALK_IN or PICKUP_DELIVERY
 * @param name        display name
 * @param fee         fee added to the total
 * @param requiresPin whether 04a (pin) comes next instead of 04b
 */
public record ServiceOptionResponse(ServiceCode code, String name, BigDecimal fee, boolean requiresPin) {

    /**
     * @param service a service type
     * @return the DTO
     */
    // OOP: Polymorphism — fee and pin rule come from the ServiceType subclass.
    public static ServiceOptionResponse from(ServiceType service) {
        return new ServiceOptionResponse(service.getCode(), service.getDisplayName(), service.getServiceFee(),
                service.requiresPinnedLocation());
    }
}
