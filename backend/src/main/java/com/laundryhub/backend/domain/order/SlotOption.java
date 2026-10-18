package com.laundryhub.backend.domain.order;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import com.laundryhub.backend.domain.common.Check;

/**
 * One time chip on 04a ("Pickup time") or 04b ("Drop-off time"), e.g. "Today, 2–4 PM".
 *
 * @param dayOffset 0 = today, 1 = tomorrow
 * @param label     chip text after the day, e.g. "2–4 PM" or "morning"
 * @param start     window start, or {@code null} for a part-of-day chip
 * @param end       window end, or {@code null} for a part-of-day chip
 */
public record SlotOption(int dayOffset, String label, LocalTime start, LocalTime end) {

    /** Validates the option. */
    public SlotOption {
        if (dayOffset < 0) {
            throw new IllegalArgumentException("Day offset must not be negative");
        }
        Check.text(label, "Slot label", TimeSlot.MAX_LABEL_LENGTH);
    }

    /**
     * @param label chip text
     * @return a part-of-day chip for today
     */
    public static SlotOption today(String label) {
        return new SlotOption(0, label, null, null);
    }

    /**
     * @param label chip text
     * @return a part-of-day chip for tomorrow
     */
    public static SlotOption tomorrow(String label) {
        return new SlotOption(1, label, null, null);
    }

    /**
     * @param today the current date
     * @return the concrete slot this chip stands for
     */
    public TimeSlot on(LocalDate today) {
        return withDate(Check.notNull(today, "Today").plusDays(dayOffset));
    }

    /**
     * @param date the day the customer chose
     * @return this chip's window on that day
     */
    public TimeSlot withDate(LocalDate date) {
        return new TimeSlot(date, label, start, end);
    }

    /**
     * @param wanted a label sent by the app
     * @return this option if its label matches
     */
    Optional<SlotOption> matching(String wanted) {
        return label.equals(wanted) ? Optional.of(this) : Optional.empty();
    }
}
