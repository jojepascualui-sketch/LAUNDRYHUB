package com.laundryhub.backend.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * The kilos of one order (screen 03). A customer cannot book less than the minimum load;
 * a weighed load that turns out lighter is still charged as the minimum
 * ("Minimum of 8 kg per order. Below 8 kg is charged as 8 kg.").
 */
// OOP: Encapsulation — the weight is validated once and the billable kilos are derived, never set.
public final class LaundryLoad {

    /** Minimum load in kilos (design default; the admin can change it on A3). */
    public static final BigDecimal MIN_KG = new BigDecimal("8");

    /** Decimal places kept for kilos (DECIMAL(6,2) in the schema). */
    public static final int KG_SCALE = 2;

    private final BigDecimal weightKg;
    private final BigDecimal minKg;

    /**
     * Creates a booked load with the default {@link #MIN_KG minimum}.
     *
     * @param weightKg kilos chosen by the customer
     * @throws InvalidWeightException if the weight is missing, not positive, or below 8 kg
     */
    public LaundryLoad(BigDecimal weightKg) {
        this(weightKg, MIN_KG);
    }

    /**
     * Creates a booked load with the admin's minimum load.
     *
     * @param weightKg kilos chosen by the customer
     * @param minKg    minimum load from the pricing settings
     * @throws InvalidWeightException if the weight is missing, not positive, or below {@code minKg}
     */
    public LaundryLoad(BigDecimal weightKg, BigDecimal minKg) {
        this(weightKg, minKg, false);
    }

    private LaundryLoad(BigDecimal weightKg, BigDecimal minKg, boolean allowBelowMinimum) {
        if (minKg == null || minKg.signum() <= 0) {
            throw new InvalidWeightException("Minimum load must be greater than 0 kg");
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new InvalidWeightException("Laundry weight must be greater than 0 kg");
        }
        if (!allowBelowMinimum && weightKg.compareTo(minKg) < 0) {
            throw new InvalidWeightException("Minimum of " + minKg.stripTrailingZeros().toPlainString()
                    + " kg per order");
        }
        this.weightKg = weightKg.setScale(KG_SCALE, RoundingMode.HALF_UP);
        this.minKg = minKg.setScale(KG_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Creates a load from the scale at pickup or drop-off. A load lighter than the minimum
     * is accepted and charged as the minimum.
     *
     * @param measuredKg kilos on the scale
     * @param minKg      minimum load from the pricing settings
     * @return the measured load
     * @throws InvalidWeightException if the weight is missing or not positive
     */
    public static LaundryLoad measured(BigDecimal measuredKg, BigDecimal minKg) {
        return new LaundryLoad(measuredKg, minKg, true);
    }

    /** @return the actual kilos */
    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /** @return the minimum load that applied */
    public BigDecimal getMinKg() {
        return minKg;
    }

    /** @return kilos that are charged: the weight, but never less than the minimum */
    public BigDecimal getBillableKg() {
        return weightKg.max(minKg);
    }

    /** @return whether the load is lighter than the minimum (and charged as the minimum) */
    public boolean isBelowMinimum() {
        return weightKg.compareTo(minKg) < 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LaundryLoad other
                && weightKg.compareTo(other.weightKg) == 0
                && minKg.compareTo(other.minKg) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(weightKg, minKg);
    }

    @Override
    public String toString() {
        return weightKg.stripTrailingZeros().toPlainString() + " kg";
    }
}
