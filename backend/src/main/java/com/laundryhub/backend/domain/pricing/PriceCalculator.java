package com.laundryhub.backend.domain.pricing;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.ServiceType;

/**
 * The one place prices are calculated. The server is the source of truth: clients may show
 * an estimate, but the saved and charged total always comes from here.
 *
 * <pre>
 * total = billableKg × ratePerKg + billableKg × soap add-on + service fee
 * </pre>
 */
public class PriceCalculator {

    // OOP: Encapsulation — the rate is fixed for the lifetime of the calculator.
    private final BigDecimal ratePerKg;

    /**
     * Creates a calculator for the current pricing settings.
     *
     * @param settings pricing settings (A3)
     */
    public PriceCalculator(PricingSettings settings) {
        this(Check.notNull(settings, "Pricing settings").getRatePerKg());
    }

    /**
     * Creates a calculator for a given price per kilo.
     *
     * @param ratePerKg price per kilo, more than 0
     */
    public PriceCalculator(BigDecimal ratePerKg) {
        this.ratePerKg = Money.of(Check.positive(ratePerKg, "Price per kilo"));
    }

    /**
     * Calculates the itemized price of an order.
     *
     * @param load    the kilos; below-minimum loads are charged as the minimum
     * @param soap    the chosen soap; must be available
     * @param service Walk-in or Pickup &amp; Delivery
     * @return the itemized breakdown
     * @throws IllegalArgumentException if an argument is missing or the soap is unavailable
     */
    public PriceBreakdown calculate(LaundryLoad load, Soap soap, ServiceType service) {
        Check.notNull(load, "Laundry load");
        Check.notNull(soap, "Fabric soap");
        Check.notNull(service, "Service");
        if (!soap.isAvailable()) {
            throw new IllegalArgumentException(soap.getName() + " is not available");
        }
        BigDecimal kg = load.getBillableKg();
        BigDecimal laundryCost = Money.of(kg.multiply(ratePerKg));
        BigDecimal soapCost = Money.of(kg.multiply(soap.getAddOnPerKg()));
        // OOP: Polymorphism — each ServiceType supplies its own fee.
        BigDecimal serviceFee = Money.of(service.getServiceFee());
        return new PriceBreakdown(service.getCode(), service.getDisplayName(), kg, ratePerKg, laundryCost,
                soap.getName(), soap.getAddOnPerKg(), soapCost, serviceFee,
                laundryCost.add(soapCost).add(serviceFee));
    }

    /**
     * Re-prices a placed order for a new weight using the rates saved on it, so price changes
     * made later on A3 never alter an order that was already placed.
     *
     * @param original the order's saved breakdown
     * @param load     the weighed load (below the minimum is charged as the minimum)
     * @return the new breakdown
     */
    public static PriceBreakdown reprice(PriceBreakdown original, LaundryLoad load) {
        Check.notNull(original, "Original price");
        BigDecimal kg = Check.notNull(load, "Laundry load").getBillableKg();
        BigDecimal laundryCost = Money.of(kg.multiply(original.ratePerKg()));
        BigDecimal soapCost = Money.of(kg.multiply(original.soapAddOnPerKg()));
        return new PriceBreakdown(original.serviceCode(), original.serviceName(), kg, original.ratePerKg(),
                laundryCost, original.soapName(), original.soapAddOnPerKg(), soapCost, original.serviceFee(),
                laundryCost.add(soapCost).add(original.serviceFee()));
    }

    /** @return price per kilo used by this calculator */
    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }
}
