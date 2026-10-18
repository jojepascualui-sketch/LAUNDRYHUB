package com.laundryhub.backend.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.laundryhub.backend.domain.order.TimeSlot;

/**
 * A pickup or drop-off time, e.g. "Today, 2–4 PM". Also used for the time chips on 04a/04b.
 *
 * @param date  the day
 * @param label chip text after the day ({@code slotLabel} when ordering)
 * @param start window start, or {@code null}
 * @param end   window end, or {@code null}
 * @param text  display text relative to today, e.g. "Tomorrow, morning"
 */
public record ScheduleResponse(LocalDate date, String label, LocalTime start, LocalTime end, String text) {

    /**
     * @param slot  a time slot
     * @param today the current date (for "Today"/"Tomorrow")
     * @return the DTO
     */
    public static ScheduleResponse from(TimeSlot slot, LocalDate today) {
        return new ScheduleResponse(slot.getDate(), slot.getLabel(), slot.getStart(), slot.getEnd(),
                slot.displayText(today));
    }
}
