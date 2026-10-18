package com.laundryhub.backend.domain.order;

import java.math.BigDecimal;
import java.util.List;

import com.laundryhub.backend.domain.common.Money;

/** Walk-in: "Bring your laundry to the shop and pick it up when it is ready." — No extra fee. */
// OOP: Inheritance — a concrete ServiceType.
public final class WalkInService extends ServiceType {

    /** Drop-off chips on 04b, exactly as in the design. */
    public static final List<SlotOption> DROP_OFF_SLOTS = List.of(
            SlotOption.today("morning"), SlotOption.today("afternoon"),
            SlotOption.tomorrow("morning"), SlotOption.tomorrow("afternoon"));

    /** Name shown on screen 02 and in summaries. */
    public static final String DISPLAY_NAME = "Walk-in";

    @Override
    public ServiceCode getCode() {
        return ServiceCode.WALK_IN;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    // OOP: Polymorphism — walk-in adds nothing to the total.
    @Override
    public BigDecimal getServiceFee() {
        return Money.ZERO;
    }

    @Override
    public boolean requiresPinnedLocation() {
        return false;
    }

    @Override
    public boolean createsRiderJobs() {
        return false;
    }

    @Override
    public List<SlotOption> getSlotOptions() {
        return DROP_OFF_SLOTS;
    }
}
