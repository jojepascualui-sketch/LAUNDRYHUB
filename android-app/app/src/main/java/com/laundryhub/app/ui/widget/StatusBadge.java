package com.laundryhub.app.ui.widget;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.laundryhub.app.R;

/**
 * Order status badges with the design's colors. A badge always shows the status text, never
 * color alone. Apply to a TextView styled {@code Widget.LaundryHub.StatusBadge}.
 *
 * <p>{@code CANCELLED} has no design yet (CLAUDE.md §7) and is deliberately not listed.</p>
 */
public enum StatusBadge {
    /** Pending: #EEF1EE / #3A4651. */
    PENDING(R.string.status_pending, R.color.status_pending_bg, R.color.status_pending_text),
    /** Washing: #E4EEF8 / #174B80. */
    WASHING(R.string.status_washing, R.color.status_washing_bg, R.color.status_washing_text),
    /** Ready: #DFF3F0 / #0B5B54. */
    READY(R.string.status_ready, R.color.status_ready_bg, R.color.status_ready_text),
    /** Out for delivery: #FFF1E8 / #8A3A12. */
    OUT_FOR_DELIVERY(R.string.status_out_for_delivery, R.color.status_out_for_delivery_bg,
            R.color.status_out_for_delivery_text),
    /** Completed: #E6F2EA / #1D5E36. */
    COMPLETED(R.string.status_completed, R.color.status_completed_bg, R.color.status_completed_text);

    // OOP: Encapsulation — each status owns its label and colors.
    @StringRes
    private final int label;
    @ColorRes
    private final int background;
    @ColorRes
    private final int text;

    StatusBadge(@StringRes int label, @ColorRes int background, @ColorRes int text) {
        this.label = label;
        this.background = background;
        this.text = text;
    }

    /**
     * Sets the badge text and colors.
     *
     * @param badge a TextView with style {@code Widget.LaundryHub.StatusBadge}
     */
    public void apply(@NonNull TextView badge) {
        badge.setText(label);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), text));
        badge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(badge.getContext(), background)));
    }

    /** @return the badge text resource */
    @StringRes
    public int getLabel() {
        return label;
    }

    /**
     * @param apiStatus status from the API, e.g. "OUT_FOR_DELIVERY"
     * @return the badge
     * @throws IllegalArgumentException for an unknown status or CANCELLED (no design yet)
     */
    @NonNull
    public static StatusBadge fromApi(@NonNull String apiStatus) {
        return valueOf(apiStatus);
    }
}
