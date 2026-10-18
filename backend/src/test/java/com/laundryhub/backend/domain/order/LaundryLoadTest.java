package com.laundryhub.backend.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LaundryLoadTest {

    @Test
    void minimumIsEightKilos() {
        assertThat(LaundryLoad.MIN_KG).isEqualByComparingTo("8");
    }

    @Test
    void acceptsExactlyTheMinimum() {
        LaundryLoad load = new LaundryLoad(new BigDecimal("8"));

        assertThat(load.getWeightKg()).isEqualByComparingTo("8");
        assertThat(load.getBillableKg()).isEqualByComparingTo("8");
        assertThat(load.isBelowMinimum()).isFalse();
    }

    @Test
    void billsAnAboveMinimumLoadByItsWeight() {
        assertThat(new LaundryLoad(new BigDecimal("12")).getBillableKg()).isEqualByComparingTo("12");
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.99", "7", "1"})
    void rejectsABookingBelowEightKilos(String kg) {
        assertThatThrownBy(() -> new LaundryLoad(new BigDecimal(kg)))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("Minimum of 8 kg per order");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsZeroOrNegativeWeight(String kg) {
        assertThatThrownBy(() -> new LaundryLoad(new BigDecimal(kg)))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void rejectsMissingWeight() {
        assertThatThrownBy(() -> new LaundryLoad(null)).isInstanceOf(InvalidWeightException.class);
    }

    @Test
    void usesTheAdminMinimumWhenGiven() {
        BigDecimal adminMinimum = new BigDecimal("10");

        assertThatThrownBy(() -> new LaundryLoad(new BigDecimal("9"), adminMinimum))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("Minimum of 10 kg per order");
        assertThat(new LaundryLoad(new BigDecimal("10"), adminMinimum).getBillableKg()).isEqualByComparingTo("10");
    }

    @Test
    void chargesALighterWeighedLoadAsEightKilos() {
        LaundryLoad weighed = LaundryLoad.measured(new BigDecimal("6.5"), LaundryLoad.MIN_KG);

        assertThat(weighed.getWeightKg()).isEqualByComparingTo("6.5");
        assertThat(weighed.getBillableKg()).isEqualByComparingTo("8");
        assertThat(weighed.isBelowMinimum()).isTrue();
    }

    @Test
    void stillRejectsAnEmptyWeighedLoad() {
        assertThatThrownBy(() -> LaundryLoad.measured(BigDecimal.ZERO, LaundryLoad.MIN_KG))
                .isInstanceOf(InvalidWeightException.class);
    }
}
