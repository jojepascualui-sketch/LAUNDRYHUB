package com.laundryhub.app.model.rider;

import androidx.annotation.NonNull;

/**
 * One checklist row of R2/R3. Titles and button labels come from the server, which uses the exact
 * DESIGN_SPEC.md wording.
 *
 * @param title       e.g. "Arrived at pickup point"
 * @param actionLabel big button text while this step is current, e.g. "I’ve arrived"
 */
public record JobStep(@NonNull String title, @NonNull String actionLabel) {

    /** Validates the texts. */
    public JobStep {
        if (title == null || title.isBlank() || actionLabel == null || actionLabel.isBlank()) {
            throw new IllegalArgumentException("Step title and action are required");
        }
    }
}
