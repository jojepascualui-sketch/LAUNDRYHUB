package com.laundryhub.backend.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Shared argument checks used by every constructor and validating method. */
class CheckTest {

    private static final int MAX = 5;

    @Test
    void notNullReturnsTheValueOrNamesTheField() {
        assertThat(Check.notNull("x", "Thing")).isEqualTo("x");
        assertThatThrownBy(() -> Check.notNull(null, "Thing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Thing is required");
    }

    @Test
    void textIsTrimmedAndLimited() {
        assertThat(Check.text("  Ana  ", "Name", MAX)).isEqualTo("Ana");
        assertThat(Check.text("12345", "Name", MAX)).isEqualTo("12345");
        assertThatThrownBy(() -> Check.text("123456", "Name", MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be at most 5 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankTextIsRejected(String value) {
        assertThatThrownBy(() -> Check.text(value, "Name", MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must not be blank");
    }

    @Test
    void optionalTextTurnsBlankIntoNull() {
        assertThat(Check.optionalText(null, "Note", MAX)).isNull();
        assertThat(Check.optionalText("  ", "Note", MAX)).isNull();
        assertThat(Check.optionalText(" ok ", "Note", MAX)).isEqualTo("ok");
    }

    @Test
    void positiveRejectsZeroNegativeAndNull() {
        assertThat(Check.positive(new BigDecimal("0.01"), "Kg")).isEqualByComparingTo("0.01");
        assertThatThrownBy(() -> Check.positive(BigDecimal.ZERO, "Kg")).hasMessage("Kg must be greater than 0");
        assertThatThrownBy(() -> Check.positive(new BigDecimal("-1"), "Kg")).hasMessage("Kg must be greater than 0");
        assertThatThrownBy(() -> Check.positive(null, "Kg")).hasMessage("Kg is required");
    }

    @Test
    void nonNegativeAllowsZero() {
        assertThat(Check.nonNegative(BigDecimal.ZERO, "Fee")).isEqualByComparingTo("0");
        assertThatThrownBy(() -> Check.nonNegative(new BigDecimal("-0.01"), "Fee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fee must not be negative");
    }

    @Test
    void acceptsAPhilippineMobileNumber() {
        assertThat(Check.mobileNumber("09171234567", "Mobile")).isEqualTo("09171234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"9171234567", "0917123456", "091712345678", "08171234567", "0917123456a", "+639171234567"})
    void rejectsOtherMobileFormats(String value) {
        assertThatThrownBy(() -> Check.mobileNumber(value, "Mobile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mobile must look like 09XXXXXXXXX");
    }

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(Check.email("  Ana@Example.COM ", 190)).isEqualTo("ana@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@example", "ana example@x.com", "@example.com", "ana@@example.com"})
    void rejectsInvalidEmails(String value) {
        assertThatThrownBy(() -> Check.email(value, 190))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is not a valid address");
    }

    @Test
    void rejectsBlankOrTooLongEmails() {
        assertThatThrownBy(() -> Check.email(" ", 190)).hasMessage("Email must not be blank");
        assertThatThrownBy(() -> Check.email("a@b.co", MAX)).hasMessage("Email must be at most 5 characters");
    }
}
