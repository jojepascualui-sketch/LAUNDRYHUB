package com.laundryhub.backend.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money rules: pesos as {@link BigDecimal} with two decimals, rounded half-up. */
public final class Money {

    /** Decimal places stored for every peso amount (DECIMAL(10,2) in the schema). */
    public static final int SCALE = 2;

    /** Rounding used for every peso amount. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** ₱0.00. */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private Money() {
    }

    /**
     * Rounds an amount to whole centavos.
     *
     * @param amount amount in pesos
     * @return the amount with two decimals
     */
    public static BigDecimal of(BigDecimal amount) {
        return Check.notNull(amount, "Amount").setScale(SCALE, ROUNDING);
    }
}
