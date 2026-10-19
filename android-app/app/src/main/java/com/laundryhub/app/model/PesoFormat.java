package com.laundryhub.app.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Peso formatting from DESIGN_SPEC.md section 1: {@code ₱1,234.00}; short form {@code ₱30} for rates. */
public final class PesoFormat {

    /** The peso sign. */
    public static final String SYMBOL = "₱";

    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private PesoFormat() {
    }

    /**
     * @param amount pesos
     * @return e.g. "₱1,234.00" (two decimals, comma thousands, half-up)
     */
    @NonNull
    public static String amount(@NonNull BigDecimal amount) {
        return SYMBOL + pattern("#,##0.00").format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * @param amount pesos
     * @return e.g. "₱30" or "₱7.5" (no trailing zeros), as the design shows rates
     */
    @NonNull
    public static String compact(@NonNull BigDecimal amount) {
        return SYMBOL + number(amount);
    }

    /**
     * @param value a quantity such as kilos
     * @return e.g. "10" or "11.5"
     */
    @NonNull
    public static String number(@NonNull BigDecimal value) {
        return pattern("#,##0.##").format(value);
    }

    private static DecimalFormat pattern(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, SYMBOLS);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
