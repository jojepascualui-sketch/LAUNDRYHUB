package com.laundryhub.app.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

/**
 * A fabric soap card on 03, as served by {@code GET /api/pricing}.
 *
 * @param id          soap id
 * @param name        e.g. "Hypoallergenic"
 * @param description card text
 * @param addOnPerKg  pesos per kilo; 0 shows "Included"
 * @param tintHex     icon tile color "#RRGGBB"
 */
public record Soap(long id, @NonNull String name, @NonNull String description, @NonNull BigDecimal addOnPerKg,
                   @NonNull String tintHex) {

    /** Validates the values. */
    public Soap {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Soap name is required");
        }
        if (addOnPerKg == null || addOnPerKg.signum() < 0) {
            throw new IllegalArgumentException("Soap add-on must be 0 or more");
        }
        description = description == null ? "" : description;
        tintHex = tintHex == null ? "" : tintHex;
    }

    /** @return whether the soap costs nothing extra ("Included") */
    public boolean isIncluded() {
        return addOnPerKg.signum() == 0;
    }
}
