package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * R1/R5 On duty / Off duty switch.
 *
 * @param onDuty new switch position
 */
public record DutyRequest(@NotNull Boolean onDuty) {
}
