package com.laundryhub.admin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void pesoHasTwoDecimalsAndCommas() {
        assertEquals("\u20B12,375.00", Money.peso(new BigDecimal("2375")));
        assertEquals("\u20B10.01", Money.peso(new BigDecimal("0.005")));
        assertEquals("\u20B10.00", Money.peso(null));
    }

    @Test
    void compactAndNumberDropTrailingZeros() {
        assertEquals("\u20B130", Money.pesoCompact(new BigDecimal("30.00")));
        assertEquals("62", Money.number(new BigDecimal("62.00")));
        assertEquals("11.5", Money.number(new BigDecimal("11.50")));
    }
}
