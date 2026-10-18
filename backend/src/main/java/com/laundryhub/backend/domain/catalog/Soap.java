package com.laundryhub.backend.domain.catalog;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A fabric soap choice (screen 03 cards, A3 "Fabric soaps" table).
 */
@Entity
@Table(name = "soaps")
public class Soap extends AuditedEntity {

    /** Longest soap name accepted. */
    public static final int MAX_NAME_LENGTH = 60;

    /** Longest card description accepted. */
    public static final int MAX_DESCRIPTION_LENGTH = 120;

    /** Icon tile color used when the admin adds a soap ({@code accentTint}). */
    public static final String DEFAULT_TINT = "#E4EEF8";

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // OOP: Encapsulation — price and availability change only through validating methods.
    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH, unique = true)
    private String name;

    @Column(name = "description", nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "add_on_per_kg", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal addOnPerKg;

    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Column(name = "tint_hex", nullable = false, length = 7)
    private String tintHex = DEFAULT_TINT;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** For JPA only. */
    protected Soap() {
    }

    /**
     * Creates an available soap.
     *
     * @param name        e.g. "Hypoallergenic"
     * @param description card text, e.g. "Gentle on sensitive skin"
     * @param addOnPerKg  extra pesos per kilo; 0 is shown as "Included"
     * @param tintHex     icon tile color "#RRGGBB"
     * @param sortOrder   position in the 2×2 grid and the A3 table
     */
    public Soap(String name, String description, BigDecimal addOnPerKg, String tintHex, int sortOrder) {
        rename(name);
        changeDescription(description);
        changeAddOnPerKg(addOnPerKg);
        changeTint(tintHex);
        moveTo(sortOrder);
    }

    /**
     * Renames the soap (A3 table).
     *
     * @param name new name
     */
    public final void rename(String name) {
        this.name = Check.text(name, "Soap name", MAX_NAME_LENGTH);
    }

    /**
     * Moves the soap to another position in the grid and table.
     *
     * @param sortOrder new position, 0 or more
     */
    public final void moveTo(int sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must not be negative");
        }
        this.sortOrder = sortOrder;
    }

    /**
     * Sets the price added per kilo (A3 "Add-on per kg").
     *
     * @param addOnPerKg pesos per kilo, zero or more
     */
    public final void changeAddOnPerKg(BigDecimal addOnPerKg) {
        this.addOnPerKg = Money.of(Check.nonNegative(addOnPerKg, "Add-on per kg"));
    }

    /**
     * Sets the card description.
     *
     * @param description new text
     */
    public final void changeDescription(String description) {
        this.description = Check.text(description, "Soap description", MAX_DESCRIPTION_LENGTH);
    }

    /**
     * Sets the icon tile color.
     *
     * @param tintHex color "#RRGGBB"
     */
    public final void changeTint(String tintHex) {
        if (tintHex == null || !HEX_COLOR.matcher(tintHex).matches()) {
            throw new IllegalArgumentException("Tint must be a color like #E4EEF8");
        }
        this.tintHex = tintHex.toUpperCase();
    }

    /**
     * Ticks or clears the A3 "Available" checkbox. Unavailable soaps cannot be ordered.
     *
     * @param available new availability
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /** @return whether the soap costs nothing extra (shown as "Included") */
    public boolean isIncluded() {
        return addOnPerKg.signum() == 0;
    }

    /** @return soap name */
    public String getName() {
        return name;
    }

    /** @return card description */
    public String getDescription() {
        return description;
    }

    /** @return pesos added per kilo */
    public BigDecimal getAddOnPerKg() {
        return addOnPerKg;
    }

    /** @return whether customers can choose this soap */
    public boolean isAvailable() {
        return available;
    }

    /** @return icon tile color "#RRGGBB" */
    public String getTintHex() {
        return tintHex;
    }

    /** @return display position */
    public int getSortOrder() {
        return sortOrder;
    }
}
