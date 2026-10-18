package com.laundryhub.backend.domain.rider;

import com.laundryhub.backend.domain.common.Check;

/**
 * One row of the active-job checklist (R2/R3) and the label of the big button that completes it.
 *
 * @param title       checklist text, e.g. "Arrived at pickup point"
 * @param actionLabel button text while this step is current, e.g. "I’ve arrived"
 */
public record JobStep(String title, String actionLabel) {

    /** Validates both texts. */
    public JobStep {
        Check.text(title, "Step title", Integer.MAX_VALUE);
        Check.text(actionLabel, "Step action", Integer.MAX_VALUE);
    }
}
