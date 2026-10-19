package com.laundryhub.app.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.math.BigDecimal;

public class PesoFormatTest {

    @Test
    public void amountsHaveTwoDecimalsAndCommas() {
        assertEquals("₱1,234.50", PesoFormat.amount(new BigDecimal("1234.5")));
        assertEquals("₱410.00", PesoFormat.amount(new BigDecimal("410")));
        assertEquals("₱0.01", PesoFormat.amount(new BigDecimal("0.005")));
    }

    @Test
    public void ratesAreShortLikeTheDesign() {
        assertEquals("₱30", PesoFormat.compact(new BigDecimal("30.00")));
        assertEquals("₱7.5", PesoFormat.compact(new BigDecimal("7.50")));
        assertEquals("10", PesoFormat.number(new BigDecimal("10.00")));
    }
}
