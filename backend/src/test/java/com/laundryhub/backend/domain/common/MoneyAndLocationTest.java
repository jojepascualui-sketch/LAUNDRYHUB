package com.laundryhub.backend.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import com.laundryhub.backend.testsupport.Fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Money rounding (₱1,234.00, HALF_UP) and the validated GPS value object. */
class MoneyAndLocationTest {

    private static final double KM_TOLERANCE = 0.05;

    @ParameterizedTest
    @CsvSource({"2.345, 2.35", "2.344, 2.34", "10, 10.00", "-1.005, -1.01", "0.005, 0.01"})
    void moneyRoundsHalfUpToTwoDecimals(String amount, String expected) {
        BigDecimal money = Money.of(new BigDecimal(amount));

        assertThat(money).isEqualTo(new BigDecimal(expected));
        assertThat(money.scale()).isEqualTo(Money.SCALE);
    }

    @Test
    void moneyRejectsNullAndHasATwoDecimalZero() {
        assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount is required");
        assertThat(Money.ZERO).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void locationKeepsSixDecimals() {
        Location pin = new Location(new BigDecimal("14.12345678"), new BigDecimal("121.1234565"), "  Brgy. 1 ");

        assertThat(pin.getLatitude()).isEqualTo(new BigDecimal("14.123457"));
        assertThat(pin.getLongitude()).isEqualTo(new BigDecimal("121.123457"));
        assertThat(pin.getAddress()).isEqualTo("Brgy. 1");
    }

    @Test
    void locationAcceptsTheEdgesOfTheGlobe() {
        Location corner = new Location(new BigDecimal("-90"), new BigDecimal("180"), null);

        assertThat(corner.getLatitude()).isEqualByComparingTo("-90");
        assertThat(corner.getAddress()).isNull();
    }

    @Test
    void locationRejectsOutOfRangeOrMissingCoordinates() {
        assertThatThrownBy(() -> new Location(new BigDecimal("90.0000001"), BigDecimal.ONE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Latitude must be between -90 and 90");
        assertThatThrownBy(() -> new Location(BigDecimal.ONE, new BigDecimal("-181"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Longitude must be between -180 and 180");
        assertThatThrownBy(() -> new Location(null, BigDecimal.ONE, null)).hasMessage("Latitude is required");
        assertThatThrownBy(() -> new Location(BigDecimal.ONE, BigDecimal.ONE, "x".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankAddressIsStoredAsNull() {
        assertThat(new Location(BigDecimal.ONE, BigDecimal.ONE, "  ").getAddress()).isNull();
    }

    @Test
    void distanceUsesTheHaversineFormula() {
        Location shop = Fixtures.shop();
        Location near = Fixtures.nearPin();

        assertThat(shop.distanceKmTo(shop)).isZero();
        assertThat(shop.distanceKmTo(near)).isCloseTo(1.5, within(KM_TOLERANCE));
        assertThat(near.distanceKmTo(shop)).isCloseTo(shop.distanceKmTo(near), within(1e-9));
        assertThat(shop.distanceKmTo(Fixtures.farPin())).isGreaterThan(5.0);
        assertThatThrownBy(() -> shop.distanceKmTo(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalLocationsCompareCoordinatesByValue() {
        Location a = new Location(new BigDecimal("14.6"), new BigDecimal("121"), "Shop");
        Location b = new Location(new BigDecimal("14.600000"), new BigDecimal("121.000"), "Shop");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(new Location(new BigDecimal("14.6"), new BigDecimal("121"), "Other"));
    }
}
