package com.laundryhub.app.testing;

import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.tracking.TrackStep;
import com.laundryhub.app.model.tracking.Tracking;

import java.util.ArrayList;
import java.util.List;

/** 07 in the design's state: LH-0001, "Out for delivery" current, rider sharing live GPS. */
public final class SampleTracking {

    private SampleTracking() {
    }

    /**
     * @param current index of the current step (0–4)
     * @return the tracking model
     */
    public static Tracking at(int current) {
        List<Tracking.Row> rows = new ArrayList<>();
        for (TrackStep step : TrackStep.values()) {
            int i = step.ordinal();
            Tracking.State state = i < current ? Tracking.State.DONE
                    : i == current ? Tracking.State.CURRENT : Tracking.State.UPCOMING;
            rows.add(new Tracking.Row(step, state));
        }
        return new Tracking(1, "LH-0001", "Out for delivery", false, rows,
                new Tracking.Rider("[RIDER NAME]", "RM", "[PLATE NO.]", "09170000001"), true,
                Location.of(14.6050, 121.0150), Location.of(14.6, 121.0), Location.of(14.6091, 121.0223));
    }
}
