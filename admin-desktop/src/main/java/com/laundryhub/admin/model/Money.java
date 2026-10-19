package com.laundryhub.admin.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Peso and kilo formatting as in the design: "₱1,234.00", "₱30", "62 kg". */
public final class Money {

    /** Peso sign. */
    public static final String PESO = "\u20B1";
    private static final int CENTS = 2;
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private Money() {
    }

    /**
     * @param amount pesos, may be {@code null} (shown as zero)
     * @return e.g. "₱2,375.00"
     */
    public static String peso(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return PESO + format("#,##0.00").format(value.setScale(CENTS, RoundingMode.HALF_UP));
    }

    /**
     * @param amount pesos
     * @return e.g. "₱30" or "₱7.5" (rates and add-ons)
     */
    public static String pesoCompact(BigDecimal amount) {
        return PESO + number(amount);
    }

    /**
     * @param value a quantity such as kilos, may be {@code null}
     * @return e.g. "10" or "11.5"
     */
    public static String number(BigDecimal value) {
        return format("#,##0.##").format(value == null ? BigDecimal.ZERO : value);
    }

    private static DecimalFormat format(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, SYMBOLS);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
