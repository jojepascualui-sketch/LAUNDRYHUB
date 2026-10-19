package com.laundryhub.app.ui.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.laundryhub.app.R;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.ServiceType;

import org.junit.Test;

import java.math.BigDecimal;

/** Continue is disabled exactly when the prototype disables it (plus GCash needs a number). */
public class WizardStepTest {

    private static final OrderDraft.Slot SLOT = new OrderDraft.Slot("2026-10-19", "2–4 PM", "Today, 2–4 PM");
    private final OrderDraft base = OrderDraft.start(8, 1L);

    @Test
    public void serviceStepNeedsAService() {
        assertFalse(WizardStep.SERVICE.isComplete(base));
        assertTrue(WizardStep.SERVICE.isComplete(base.withService(ServiceType.WALK_IN)));
    }

    @Test
    public void scheduleNeedsASlotAndDeliveryAlsoAPin() {
        OrderDraft walkIn = base.withService(ServiceType.WALK_IN);
        OrderDraft delivery = base.withService(ServiceType.PICKUP_DELIVERY).withSlot(SLOT);

        assertFalse(WizardStep.SCHEDULE.isComplete(walkIn));
        assertTrue(WizardStep.SCHEDULE.isComplete(walkIn.withSlot(SLOT)));
        assertFalse(WizardStep.SCHEDULE.isComplete(delivery));
        OrderDraft pinned = delivery.withPin(Location.of(14.61, 121.01));
        assertTrue("unchecked pin is fine: the server checks again", WizardStep.SCHEDULE.isComplete(pinned));
        assertTrue(WizardStep.SCHEDULE.isComplete(pinned.withPinCheck(true)));
        assertFalse("outside the delivery radius", WizardStep.SCHEDULE.isComplete(pinned.withPinCheck(false)));
    }

    @Test
    public void gcashNeedsAMobileNumber() {
        OrderDraft gcash = base.withPayment(PaymentOption.GCASH);

        assertTrue(WizardStep.REVIEW.isComplete(base));
        assertFalse(WizardStep.REVIEW.isComplete(gcash));
        assertFalse(WizardStep.REVIEW.isComplete(gcash.withGcashNumber("0917 123")));
        assertTrue(WizardStep.REVIEW.isComplete(gcash.withGcashNumber("0917 123 4567")));
    }

    @Test
    public void titlesAndOrderFollowTheDesign() {
        assertEquals(R.string.step_title_drop_off, WizardStep.SCHEDULE.title(ServiceType.WALK_IN));
        assertEquals(R.string.step_title_pin, WizardStep.SCHEDULE.title(ServiceType.PICKUP_DELIVERY));
        assertEquals(4, WizardStep.REVIEW.number());
        assertEquals(WizardStep.LOAD, WizardStep.SERVICE.next());
        assertNull(WizardStep.SERVICE.previous());
        assertTrue(WizardStep.REVIEW.isLast());
    }
}
