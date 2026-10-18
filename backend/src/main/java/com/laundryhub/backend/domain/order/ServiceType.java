package com.laundryhub.backend.domain.order;

import java.math.BigDecimal;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.common.Check;

/**
 * A service the customer can choose on screen 02. Each service decides its own fee and
 * whether it needs a pinned location and rider jobs.
 */
// OOP: Abstraction — the price calculator and orders only know "a service type".
public abstract class ServiceType {

    /**
     * Builds the service for a code using the current prices.
     *
     * @param code     chosen service
     * @param settings current pricing settings (for the delivery fee)
     * @return the matching service
     */
    public static ServiceType of(ServiceCode code, PricingSettings settings) {
        return of(code, Check.notNull(settings, "Pricing settings").getDeliveryFee());
    }

    /**
     * Builds the service for a code with a given delivery fee (e.g. the fee saved on an order).
     *
     * @param code        chosen service
     * @param deliveryFee pickup and delivery fee in pesos
     * @return the matching service
     */
    public static ServiceType of(ServiceCode code, BigDecimal deliveryFee) {
        return switch (Check.notNull(code, "Service")) {
            case WALK_IN -> new WalkInService();
            case PICKUP_DELIVERY -> new PickupDeliveryService(deliveryFee);
        };
    }

    /** @return the stored code */
    public abstract ServiceCode getCode();

    /** @return name shown on cards and summaries, e.g. "Pickup &amp; Delivery" */
    public abstract String getDisplayName();

    /** @return the fee this service adds to the order total */
    // OOP: Polymorphism — Walk-in returns ₱0, Pickup & Delivery returns the delivery fee.
    public abstract BigDecimal getServiceFee();

    /** @return whether the customer must pin a location on the map (screen 04a) */
    public abstract boolean requiresPinnedLocation();

    /** @return whether placing the order creates PICKUP and RETURN_DELIVERY rider jobs */
    public abstract boolean createsRiderJobs();

    /** @return the time chips offered on 04a/04b, in design order */
    // OOP: Polymorphism — delivery offers pickup windows, walk-in offers parts of the day.
    public abstract List<SlotOption> getSlotOptions();

    /**
     * Finds the chip with this label.
     *
     * @param label chip text sent by the app, e.g. "2–4 PM"
     * @return the matching option
     * @throws IllegalArgumentException if the service has no such chip
     */
    public SlotOption findSlot(String label) {
        return getSlotOptions().stream()
                .flatMap(option -> option.matching(label).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\"" + label + "\" is not a time slot for "
                        + getDisplayName()));
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
