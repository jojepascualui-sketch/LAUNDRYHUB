package com.laundryhub.app.model.rider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.laundryhub.app.R;
import com.laundryhub.app.model.PaymentOption;

import org.junit.Test;

/** Payment wording per job type, exactly as source/Rider.dc.html. */
public class JobTypeTest {

    @Test
    public void pickupPaysLater() {
        assertEquals(R.string.pay_note_cash_on_return, JobType.PICKUP.paymentNote(PaymentOption.CASH));
        assertEquals(R.string.pay_note_gcash_total, JobType.PICKUP.paymentNote(PaymentOption.GCASH));
        assertEquals(R.string.done_note_dropped_at_shop, JobType.PICKUP.doneNote(PaymentOption.CASH));
        assertFalse(JobType.PICKUP.collectsCash(PaymentOption.CASH));
    }

    @Test
    public void returnDeliveryCollectsCashOnlyForCashOrders() {
        assertEquals(R.string.pay_note_collect_cash, JobType.RETURN_DELIVERY.paymentNote(PaymentOption.CASH));
        assertEquals(R.string.pay_note_paid_gcash, JobType.RETURN_DELIVERY.paymentNote(PaymentOption.GCASH));
        assertEquals(R.string.done_note_cash_collected, JobType.RETURN_DELIVERY.doneNote(PaymentOption.CASH));
        assertEquals(R.string.pay_note_paid_gcash, JobType.RETURN_DELIVERY.doneNote(PaymentOption.GCASH));
        assertTrue(JobType.RETURN_DELIVERY.collectsCash(PaymentOption.CASH));
        assertFalse(JobType.RETURN_DELIVERY.collectsCash(PaymentOption.GCASH));
    }

    @Test
    public void badgeColorsFollowTheDesign() {
        assertEquals(R.color.status_washing_bg, JobType.PICKUP.getBadgeBackground());
        assertEquals(R.color.warn_text, JobType.RETURN_DELIVERY.getBadgeText());
    }
}
