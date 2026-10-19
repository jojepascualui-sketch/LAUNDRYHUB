package com.laundryhub.app.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.math.BigDecimal;

public class LocationTest {

    @Test
    public void keepsSixDecimalsLikeTheServer() {
        Location pin = Location.of(14.6109876543, 121.0101);

        assertEquals(new BigDecimal("14.610988"), pin.getLatitude());
        assertEquals(new BigDecimal("121.010100"), pin.getLongitude());
        assertEquals(Location.of(14.610988, 121.0101), pin);
    }

    @Test(expected = IllegalArgumentException.class)
    public void latitudeMustBeInRange() {
        Location.of(91, 121);
    }

    @Test(expected = IllegalArgumentException.class)
    public void longitudeMustBeInRange() {
        new Location(BigDecimal.ONE, new BigDecimal("-180.5"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coordinatesMustBeNumbers() {
        Location.of(Double.NaN, 121);
    }
}
