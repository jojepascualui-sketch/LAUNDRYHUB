package com.laundryhub.backend.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.testsupport.Fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A3 Pricing &amp; soaps: soap and pricing-settings validation. */
class CatalogTest {

    private static final String SHOP_GCASH = "09170009999";

    private final Admin admin = Fixtures.admin();

    // ---- Soap -------------------------------------------------------------------------------

    @Test
    void soapStoresRoundedAddOnAndUpperCaseTint() {
        Soap soap = new Soap(" Eco Clean ", "Plant-based formula", new BigDecimal("4.555"), "#abcdef", 5);

        assertThat(soap.getName()).isEqualTo("Eco Clean");
        assertThat(soap.getAddOnPerKg()).isEqualTo(new BigDecimal("4.56"));
        assertThat(soap.getTintHex()).isEqualTo("#ABCDEF");
        assertThat(soap.isAvailable()).isTrue();
        assertThat(soap.isIncluded()).isFalse();
        assertThat(Fixtures.standard().isIncluded()).isTrue();
    }

    @Test
    void soapRejectsBlankOrTooLongText() {
        assertThatThrownBy(() -> new Soap(" ", "desc", BigDecimal.ZERO, Soap.DEFAULT_TINT, 1))
                .hasMessage("Soap name must not be blank");
        assertThatThrownBy(() -> new Soap("x".repeat(Soap.MAX_NAME_LENGTH + 1), "desc", BigDecimal.ZERO,
                Soap.DEFAULT_TINT, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Soap("Eco", "", BigDecimal.ZERO, Soap.DEFAULT_TINT, 1))
                .hasMessage("Soap description must not be blank");
    }

    @Test
    void soapRejectsNegativeOrMissingAddOn() {
        assertThatThrownBy(() -> new Soap("Eco", "desc", new BigDecimal("-1"), Soap.DEFAULT_TINT, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Add-on per kg must not be negative");
        assertThatThrownBy(() -> new Soap("Eco", "desc", null, Soap.DEFAULT_TINT, 1))
                .hasMessage("Add-on per kg is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"E4EEF8", "#E4EEF", "#XYZ123", "blue"})
    void soapRejectsTintsThatAreNotHexColors(String tint) {
        assertThatThrownBy(() -> new Soap("Eco", "desc", BigDecimal.ZERO, tint, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tint must be a color like #E4EEF8");
    }

    @Test
    void soapRejectsANegativeSortOrder() {
        assertThatThrownBy(() -> new Soap("Eco", "desc", BigDecimal.ZERO, Soap.DEFAULT_TINT, -1))
                .hasMessage("Sort order must not be negative");
    }

    // ---- PricingSettings --------------------------------------------------------------------

    @Test
    void defaultsMatchTheDesign() {
        PricingSettings defaults = PricingSettings.defaults();

        assertThat(defaults.getRatePerKg()).isEqualTo(new BigDecimal("30.00"));
        assertThat(defaults.getMinKg()).isEqualByComparingTo(LaundryLoad.MIN_KG);
        assertThat(defaults.getDeliveryFee()).isEqualTo(new BigDecimal("60.00"));
        assertThat(defaults.getDeliveryRadiusKm()).isEqualByComparingTo("5");
        assertThat(defaults.isCashEnabled()).isTrue();
        assertThat(defaults.isGcashEnabled()).isTrue();
        assertThat(defaults.getShopGcashNumber()).isNull();
    }

    @Test
    void adminCanChangeEveryValue() {
        PricingSettings settings = PricingSettings.defaults();

        settings.update(new BigDecimal("35.005"), new BigDecimal("6"), BigDecimal.ZERO, new BigDecimal("3"), true,
                true, SHOP_GCASH, admin);

        assertThat(settings.getRatePerKg()).isEqualTo(new BigDecimal("35.01"));
        assertThat(settings.getMinKg()).isEqualByComparingTo("6");
        assertThat(settings.getDeliveryFee()).isEqualTo(new BigDecimal("0.00"));
        assertThat(settings.getDeliveryRadiusKm()).isEqualByComparingTo("3");
        assertThat(settings.getShopGcashNumber()).isEqualTo(SHOP_GCASH);
    }

    @Test
    void invalidValueLeavesEverythingUnchanged() {
        PricingSettings settings = PricingSettings.defaults();

        // Regression: the rate is valid but the minimum is not; nothing may be half applied.
        assertThatThrownBy(() -> settings.update(new BigDecimal("40"), BigDecimal.ZERO, BigDecimal.TEN,
                BigDecimal.ONE, true, false, null, admin)).isInstanceOf(IllegalArgumentException.class);
        assertThat(settings.getRatePerKg()).isEqualByComparingTo("30");
        assertThat(settings.getMinKg()).isEqualByComparingTo(LaundryLoad.MIN_KG);
    }

    @Test
    void staffMayNotChangePrices() {
        PricingSettings settings = PricingSettings.defaults();

        assertThatThrownBy(() -> settings.update(new BigDecimal("40"), LaundryLoad.MIN_KG, BigDecimal.TEN,
                BigDecimal.ONE, true, false, null, Fixtures.staff()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Counter Staff may not change prices");
        assertThat(settings.getRatePerKg()).isEqualByComparingTo("30");
        assertThatThrownBy(() -> settings.update(new BigDecimal("40"), LaundryLoad.MIN_KG, BigDecimal.TEN,
                BigDecimal.ONE, true, false, null, null)).hasMessage("Editor is required");
    }

    @Test
    void gcashNeedsTheShopNumberAndOneMethodMustStayOn() {
        PricingSettings settings = PricingSettings.defaults();

        assertThatThrownBy(() -> settings.update(new BigDecimal("30"), LaundryLoad.MIN_KG, BigDecimal.TEN,
                BigDecimal.ONE, true, true, null, admin))
                .hasMessage("Shop GCash number is required when GCash is enabled");
        assertThatThrownBy(() -> settings.update(new BigDecimal("30"), LaundryLoad.MIN_KG, BigDecimal.TEN,
                BigDecimal.ONE, false, false, null, admin))
                .hasMessage("At least one payment method must be enabled");
        assertThatThrownBy(() -> settings.update(new BigDecimal("30"), LaundryLoad.MIN_KG, BigDecimal.TEN,
                BigDecimal.ONE, true, true, "12345", admin))
                .hasMessage("Shop GCash number must look like 09XXXXXXXXX");
    }

    @Test
    void numbersMustBeInRange() {
        PricingSettings settings = PricingSettings.defaults();
        BigDecimal ok = BigDecimal.TEN;

        assertThatThrownBy(() -> settings.update(BigDecimal.ZERO, ok, ok, ok, true, false, null, admin))
                .hasMessage("Price per kilo must be greater than 0");
        assertThatThrownBy(() -> settings.update(ok, BigDecimal.ZERO, ok, ok, true, false, null, admin))
                .hasMessage("Minimum load must be greater than 0");
        assertThatThrownBy(() -> settings.update(ok, ok, new BigDecimal("-1"), ok, true, false, null, admin))
                .hasMessage("Pickup & delivery fee must not be negative");
        assertThatThrownBy(() -> settings.update(ok, ok, ok, BigDecimal.ZERO, true, false, null, admin))
                .hasMessage("Delivery radius must be greater than 0");
    }
}
