package com.laundryhub.app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.laundryhub.app.testing.FakeOrderRepository;

import org.junit.Test;

import java.math.BigDecimal;

public class PricingTest {

    private final Pricing pricing = new FakeOrderRepository().pricing;

    @Test
    public void estimateUsesTheServerFormula() {
        // CLAUDE.md example: 10 kg, Hypoallergenic, Pickup & Delivery = 300 + 50 + 60 = 410.
        PriceLines lines = pricing.estimate(ServiceType.PICKUP_DELIVERY, 10, pricing.soap(FakeOrderRepository.HYPOALLERGENIC));

        assertEquals(new BigDecimal("300.00"), lines.laundryCost());
        assertEquals(new BigDecimal("50.00"), lines.soapCost());
        assertEquals(new BigDecimal("60.00"), lines.deliveryFee());
        assertEquals(new BigDecimal("410.00"), lines.total());
        assertFalse(lines.fromServer());
    }

    @Test
    public void noServiceYetMeansNoFeeAndBelowMinimumIsBilledAsMinimum() {
        PriceLines lines = pricing.estimate(null, 5, pricing.soap(FakeOrderRepository.STANDARD));

        assertEquals(new BigDecimal("240.00"), lines.total());
        assertEquals(0, new BigDecimal("8").compareTo(lines.billableKg()));
    }

    @Test
    public void minimumIsRoundedUpToWholeKilos() {
        Pricing odd = new Pricing(BigDecimal.TEN, new BigDecimal("7.5"), BigDecimal.ZERO, true, false, null);
        assertEquals(8, odd.minKgWhole());
        assertFalse(odd.isEnabled(PaymentOption.GCASH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rateMustBePositive() {
        new Pricing(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, true, true, null);
    }
}
