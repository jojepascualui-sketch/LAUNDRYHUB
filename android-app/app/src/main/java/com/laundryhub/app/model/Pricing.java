package com.laundryhub.app.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * Prices and soaps from {@code GET /api/pricing}. Can make a local estimate for the footer while
 * the server quote is on its way; the server's number is always the one charged.
 */
public final class Pricing {

    // OOP: Encapsulation — immutable, validated once.
    private final BigDecimal ratePerKg;
    private final BigDecimal minKg;
    private final BigDecimal deliveryFee;
    private final boolean cashEnabled;
    private final boolean gcashEnabled;
    private final List<Soap> soaps;

    /**
     * @param ratePerKg    price per kilo, more than 0
     * @param minKg        minimum load, more than 0
     * @param deliveryFee  delivery fee, 0 or more
     * @param cashEnabled  Cash offered
     * @param gcashEnabled GCash offered
     * @param soaps        available soaps in display order
     * @throws IllegalArgumentException if a value is missing or out of range
     */
    public Pricing(BigDecimal ratePerKg, BigDecimal minKg, BigDecimal deliveryFee, boolean cashEnabled,
                   boolean gcashEnabled, List<Soap> soaps) {
        if (ratePerKg == null || ratePerKg.signum() <= 0) {
            throw new IllegalArgumentException("Price per kilo must be more than 0");
        }
        if (minKg == null || minKg.signum() <= 0) {
            throw new IllegalArgumentException("Minimum load must be more than 0");
        }
        if (deliveryFee == null || deliveryFee.signum() < 0) {
            throw new IllegalArgumentException("Delivery fee must be 0 or more");
        }
        this.ratePerKg = ratePerKg;
        this.minKg = minKg;
        this.deliveryFee = deliveryFee;
        this.cashEnabled = cashEnabled;
        this.gcashEnabled = gcashEnabled;
        this.soaps = soaps == null ? Collections.emptyList() : List.copyOf(soaps);
    }

    /**
     * Local estimate with the same formula as the server:
     * billableKg × rate + billableKg × soap add-on + service fee.
     *
     * @param service chosen service, or {@code null} (no fee yet)
     * @param kg      kilos on the stepper
     * @param soap    chosen soap
     * @return the price lines (not confirmed by the server)
     */
    @NonNull
    public PriceLines estimate(@Nullable ServiceType service, int kg, @NonNull Soap soap) {
        BigDecimal billable = BigDecimal.valueOf(kg).max(minKg);
        BigDecimal laundry = money(billable.multiply(ratePerKg));
        BigDecimal soapCost = money(billable.multiply(soap.addOnPerKg()));
        // OOP: Polymorphism — the ServiceType decides its fee.
        BigDecimal fee = money(service == null ? BigDecimal.ZERO : service.fee(this));
        return new PriceLines(billable, ratePerKg, laundry, soap.name(), soap.addOnPerKg(), soapCost, fee,
                laundry.add(soapCost).add(fee), false);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** @return the minimum load in whole kilos (the − button stops here) */
    public int minKgWhole() {
        return minKg.setScale(0, RoundingMode.CEILING).intValueExact();
    }

    /**
     * @param id soap id
     * @return the soap, or {@code null}
     */
    @Nullable
    public Soap soap(long id) {
        for (Soap soap : soaps) {
            if (soap.id() == id) {
                return soap;
            }
        }
        return null;
    }

    /**
     * @param option a payment method
     * @return whether the admin has it switched on
     */
    public boolean isEnabled(@NonNull PaymentOption option) {
        return option == PaymentOption.CASH ? cashEnabled : gcashEnabled;
    }

    /** @return price per kilo */
    @NonNull
    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }

    /** @return minimum load */
    @NonNull
    public BigDecimal getMinKg() {
        return minKg;
    }

    /** @return delivery fee */
    @NonNull
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** @return available soaps in display order (read-only) */
    @NonNull
    public List<Soap> getSoaps() {
        return soaps;
    }
}
