package com.laundryhub.app.ui.rider;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;

/** The two tabs on R1, in design order. */
public enum RiderTab {

    /** Open bookings with Decline / Accept booking. */
    AVAILABLE(R.string.tab_available, R.string.empty_available),
    /** Accepted jobs with Start job / Continue job. */
    MY_JOBS(R.string.tab_my_jobs, R.string.empty_my_jobs);

    // OOP: Encapsulation — fixed texts per tab.
    @StringRes
    private final int label;
    @StringRes
    private final int emptyText;

    RiderTab(@StringRes int label, @StringRes int emptyText) {
        this.label = label;
        this.emptyText = emptyText;
    }

    /** @return "Available (%1$d)" / "My jobs (%1$d)" */
    @StringRes
    public int getLabel() {
        return label;
    }

    /** @return the empty-list card text */
    @StringRes
    public int getEmptyText() {
        return emptyText;
    }

    /**
     * @param name a tab name from an intent extra, may be {@code null}
     * @return that tab, or {@link #AVAILABLE}
     */
    @NonNull
    public static RiderTab fromName(String name) {
        for (RiderTab tab : values()) {
            if (tab.name().equals(name)) {
                return tab;
            }
        }
        return AVAILABLE;
    }
}
