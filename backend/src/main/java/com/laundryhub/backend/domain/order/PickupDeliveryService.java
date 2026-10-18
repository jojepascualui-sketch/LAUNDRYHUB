package com.laundryhub.backend.domain.order;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Money;

/**
 * Pickup &amp; Delivery: "Our rider collects it at your pinned location and returns it clean."
 * Adds the delivery fee (default ₱60).
 */
// OOP: Inheritance — a concrete ServiceType.
public final class PickupDeliveryService extends ServiceType {

    /** Pickup chips on 04a, exactly as in the design: Today 2–4 PM, 4–6 PM; Tomorrow 9–11 AM, 1–3 PM. */
    public static final List<SlotOption> PICKUP_SLOTS = List.of(
            new SlotOption(0, "2–4 PM", LocalTime.of(14, 0), LocalTime.of(16, 0)),
            new SlotOption(0, "4–6 PM", LocalTime.of(16, 0), LocalTime.of(18, 0)),
            new SlotOption(1, "9–11 AM", LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new SlotOption(1, "1–3 PM", LocalTime.of(13, 0), LocalTime.of(15, 0)));

    /** Name shown on screen 02 and in summaries. */
    public static final String DISPLAY_NAME = "Pickup & Delivery";

    // OOP: Encapsulation — the fee is fixed when the service is created.
    private final BigDecimal deliveryFee;

    /**
     * Creates the service with the current delivery fee.
     *
     * @param deliveryFee pickup and delivery fee in pesos, 0 or more
     */
    public PickupDeliveryService(BigDecimal deliveryFee) {
        this.deliveryFee = Money.of(Check.nonNegative(deliveryFee, "Delivery fee"));
    }

    @Override
    public ServiceCode getCode() {
        return ServiceCode.PICKUP_DELIVERY;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    // OOP: Polymorphism — delivery adds its fee to the total.
    @Override
    public BigDecimal getServiceFee() {
        return deliveryFee;
    }

    @Override
    public boolean requiresPinnedLocation() {
        return true;
    }

    @Override
    public boolean createsRiderJobs() {
        return true;
    }

    @Override
    public List<SlotOption> getSlotOptions() {
        return PICKUP_SLOTS;
    }
}
