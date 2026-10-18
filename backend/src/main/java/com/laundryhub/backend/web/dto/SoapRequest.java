package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A3 "+ Add soap" and soap row edits.
 *
 * @param name        name
 * @param description card text on 03
 * @param addOnPerKg  "Add-on per kg"
 * @param available   "Available" checkbox
 * @param tintHex     icon tile color; default {@code #E4EEF8}
 * @param sortOrder   position; default = after the last soap
 */
public record SoapRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 120) String description,
        @NotNull @PositiveOrZero BigDecimal addOnPerKg,
        @NotNull Boolean available,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a color like #E4EEF8") String tintHex,
        @PositiveOrZero Integer sortOrder) {
}
