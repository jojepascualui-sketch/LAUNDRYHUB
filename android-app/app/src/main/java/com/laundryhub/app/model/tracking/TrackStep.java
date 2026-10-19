package com.laundryhub.app.model.tracking;

import androidx.annotation.StringRes;

import com.laundryhub.app.R;

/**
 * The five steps of the 07 timeline, with the design's title and note. The server maps order statuses
 * onto them (there is no "Ready" step in the design, CLAUDE.md §7).
 */
public enum TrackStep {

    /** Order received. */
    ORDER_PLACED(R.string.track_placed, R.string.track_placed_note),
    /** Rider collected the laundry. */
    PICKED_UP(R.string.track_picked_up, R.string.track_picked_up_note),
    /** Washing and drying. */
    WASHING(R.string.track_washing, R.string.track_washing_note),
    /** Rider heading to the pin. */
    OUT_FOR_DELIVERY(R.string.track_out, R.string.track_out_note),
    /** Done. */
    DELIVERED(R.string.track_delivered, R.string.track_delivered_note);

    // OOP: Encapsulation — fixed texts per step.
    @StringRes
    private final int title;
    @StringRes
    private final int note;

    TrackStep(@StringRes int title, @StringRes int note) {
        this.title = title;
        this.note = note;
    }

    /** @return "Order placed" */
    @StringRes
    public int getTitle() {
        return title;
    }

    /** @return "We received your order" */
    @StringRes
    public int getNote() {
        return note;
    }

    /**
     * @param apiKey key from the API
     * @return the step, or {@code null} if unknown
     */
    public static TrackStep fromApi(String apiKey) {
        for (TrackStep step : values()) {
            if (step.name().equals(apiKey)) {
                return step;
            }
        }
        return null;
    }
}
