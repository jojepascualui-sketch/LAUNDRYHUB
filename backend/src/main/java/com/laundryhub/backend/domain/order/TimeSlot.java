package com.laundryhub.backend.domain.order;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import com.laundryhub.backend.domain.common.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The chosen pickup time (04a, e.g. "Today, 2–4 PM") or drop-off time (04b, e.g. "Tomorrow, morning").
 * The day is stored as a date; "Today"/"Tomorrow" is worked out when it is displayed.
 */
// OOP: Encapsulation — an immutable, validated value object.
@Embeddable
public class TimeSlot {

    /** Longest slot label accepted, e.g. "2–4 PM" or "afternoon". */
    public static final int MAX_LABEL_LENGTH = 40;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate date;

    @Column(name = "schedule_label", nullable = false, length = MAX_LABEL_LENGTH)
    private String label;

    @Column(name = "schedule_start")
    private LocalTime start;

    @Column(name = "schedule_end")
    private LocalTime end;

    /** For JPA only. */
    protected TimeSlot() {
    }

    /**
     * Creates a slot with a time window, as used for pickup chips ("2–4 PM").
     *
     * @param date  the day
     * @param label chip text after the day, e.g. "2–4 PM"
     * @param start window start
     * @param end   window end, after {@code start}
     * @throws IllegalArgumentException if a value is missing or the window is empty
     */
    public TimeSlot(LocalDate date, String label, LocalTime start, LocalTime end) {
        this.date = Check.notNull(date, "Schedule date");
        this.label = Check.text(label, "Schedule label", MAX_LABEL_LENGTH);
        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException("Schedule needs both a start and an end time, or neither");
        }
        if (start != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("Schedule must start before it ends");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a part-of-day slot without exact times, as used for drop-off chips ("morning").
     *
     * @param date  the day
     * @param label chip text after the day, e.g. "morning"
     * @return the slot
     */
    public static TimeSlot partOfDay(LocalDate date, String label) {
        return new TimeSlot(date, label, null, null);
    }

    /**
     * Text as on the chips and summaries, e.g. "Today, 2–4 PM".
     *
     * @param today the current date
     * @return the display text
     */
    public String displayText(LocalDate today) {
        Check.notNull(today, "Today");
        String day;
        if (date.equals(today)) {
            day = "Today";
        } else if (date.equals(today.plusDays(1))) {
            day = "Tomorrow";
        } else {
            day = date.toString();
        }
        return day + ", " + label;
    }

    /** @return the day */
    public LocalDate getDate() {
        return date;
    }

    /** @return the chip text after the day */
    public String getLabel() {
        return label;
    }

    /** @return window start, or {@code null} for a part-of-day slot */
    public LocalTime getStart() {
        return start;
    }

    /** @return window end, or {@code null} for a part-of-day slot */
    public LocalTime getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TimeSlot other && date.equals(other.date) && label.equals(other.label)
                && Objects.equals(start, other.start) && Objects.equals(end, other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, label, start, end);
    }
}
