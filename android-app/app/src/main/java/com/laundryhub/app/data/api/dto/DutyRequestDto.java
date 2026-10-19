package com.laundryhub.app.data.api.dto;

/** Body of {@code PATCH /api/rider/duty}. */
public final class DutyRequestDto {

    // OOP: Encapsulation — an immutable request.
    private final boolean onDuty;

    /**
     * @param onDuty new switch position
     */
    public DutyRequestDto(boolean onDuty) {
        this.onDuty = onDuty;
    }
}
