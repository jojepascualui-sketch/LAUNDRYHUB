package com.laundryhub.backend.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.PickupDeliveryService;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;
import com.laundryhub.backend.domain.order.WalkInService;

import org.junit.jupiter.api.Test;

/** Totals must match the design examples (DESIGN_SPEC.md, screens 05 and A1/A2). */
class PriceCalculatorTest {

    private static final Soap STANDARD = new Soap("Standard", "Everyday clean for regular clothes",
            new BigDecimal("0"), "#E4EEF8", 1);
    private static final Soap HYPOALLERGENIC = new Soap("Hypoallergenic", "Gentle on sensitive skin",
            new BigDecimal("5"), "#E6F2EA", 2);
    private static final Soap PREMIUM_SCENTED = new Soap("Premium Scented", "Long-lasting fresh fragrance",
            new BigDecimal("8"), "#F3E8F7", 3);

    private final PricingSettings defaults = PricingSettings.defaults();
    private final PriceCalculator calculator = new PriceCalculator(defaults);
    private final ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, defaults);
    private final ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, defaults);

    private static LaundryLoad kg(String kilos) {
        return new LaundryLoad(new BigDecimal(kilos));
    }

    @Test
    void tenKilosHypoallergenicDeliveryIs410() {
        PriceBreakdown price = calculator.calculate(kg("10"), HYPOALLERGENIC, delivery);

        // Screen 05: Laundry (10 kg × ₱30) ₱300.00, Hypoallergenic (10 kg × ₱5) ₱50.00, fee ₱60.00
        assertThat(price.serviceName()).isEqualTo("Pickup & Delivery");
        assertThat(price.billableKg()).isEqualByComparingTo("10");
        assertThat(price.ratePerKg()).isEqualByComparingTo("30");
        assertThat(price.laundryCost()).isEqualTo(new BigDecimal("300.00"));
        assertThat(price.soapName()).isEqualTo("Hypoallergenic");
        assertThat(price.soapAddOnPerKg()).isEqualByComparingTo("5");
        assertThat(price.soapCost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(price.serviceFee()).isEqualTo(new BigDecimal("60.00"));
        assertThat(price.total()).isEqualTo(new BigDecimal("410.00"));
    }

    @Test
    void twelveKilosPremiumScentedDeliveryIs516() {
        PriceBreakdown price = calculator.calculate(kg("12"), PREMIUM_SCENTED, delivery);

        assertThat(price.laundryCost()).isEqualTo(new BigDecimal("360.00"));
        assertThat(price.soapCost()).isEqualTo(new BigDecimal("96.00"));
        assertThat(price.serviceFee()).isEqualTo(new BigDecimal("60.00"));
        assertThat(price.total()).isEqualTo(new BigDecimal("516.00"));
    }

    @Test
    void eightKilosStandardWalkInIs240() {
        PriceBreakdown price = calculator.calculate(kg("8"), STANDARD, walkIn);

        assertThat(price.serviceName()).isEqualTo("Walk-in");
        assertThat(price.laundryCost()).isEqualTo(new BigDecimal("240.00"));
        assertThat(price.soapCost()).isEqualTo(new BigDecimal("0.00"));
        assertThat(price.serviceFee()).isEqualTo(new BigDecimal("0.00"));
        assertThat(price.total()).isEqualTo(new BigDecimal("240.00"));
    }

    @Test
    void lighterWeighedLoadIsChargedAsEightKilos() {
        LaundryLoad weighed = LaundryLoad.measured(new BigDecimal("6"), LaundryLoad.MIN_KG);

        PriceBreakdown price = calculator.calculate(weighed, STANDARD, walkIn);

        assertThat(price.billableKg()).isEqualByComparingTo("8");
        assertThat(price.total()).isEqualTo(new BigDecimal("240.00"));
    }

    @Test
    void usesTheAdminRatesWhenTheyChange() {
        PriceCalculator custom = new PriceCalculator(new BigDecimal("35"));
        ServiceType customDelivery = new PickupDeliveryService(new BigDecimal("75"));

        PriceBreakdown price = custom.calculate(kg("10"), HYPOALLERGENIC, customDelivery);

        assertThat(price.total()).isEqualTo(new BigDecimal("475.00"));
    }

    @Test
    void walkInAddsNoFeeAndDeliveryAddsTheFee() {
        assertThat(new WalkInService().getServiceFee()).isEqualByComparingTo("0");
        assertThat(delivery.getServiceFee()).isEqualByComparingTo("60");
    }

    @Test
    void rejectsAnUnavailableSoap() {
        Soap babySafe = new Soap("Baby-Safe", "Mild and dye-free formula", new BigDecimal("6"), "#FFF1E8", 4);
        babySafe.setAvailable(false);

        assertThatThrownBy(() -> calculator.calculate(kg("8"), babySafe, walkIn))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Baby-Safe is not available");
    }

    @Test
    void rejectsANonPositiveRate() {
        assertThatThrownBy(() -> new PriceCalculator(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void breakdownRefusesLinesThatDoNotAddUp() {
        assertThatThrownBy(() -> new PriceBreakdown(ServiceCode.WALK_IN, "Walk-in", new BigDecimal("8"),
                new BigDecimal("30"), new BigDecimal("240.00"), "Standard", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("250.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
