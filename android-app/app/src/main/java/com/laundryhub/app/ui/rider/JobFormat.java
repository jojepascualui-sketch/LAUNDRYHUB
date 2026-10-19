package com.laundryhub.app.ui.rider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.laundryhub.app.R;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.rider.JobType;
import com.laundryhub.app.model.rider.RiderJob;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Texts and badges shared by the booking cards (R1), the active job (R2/R3) and R4. */
public final class JobFormat {

    /** Distances are shown with one decimal ("1.2 km"). */
    private static final int DISTANCE_DECIMALS = 1;

    private JobFormat() {
    }

    /**
     * @param context for strings
     * @param job     the job
     * @return "LH-0003 · 1.2 km", or just the order number when the distance is unknown
     */
    @NonNull
    public static String meta(@NonNull Context context, @NonNull RiderJob job) {
        BigDecimal km = job.getDistanceKm();
        if (km == null) {
            return job.getOrderNo();
        }
        String distance = context.getString(R.string.distance_km,
                km.setScale(DISTANCE_DECIMALS, RoundingMode.HALF_UP).toPlainString());
        return context.getString(R.string.booking_meta, job.getOrderNo(), distance);
    }

    /**
     * @param context for strings
     * @param job     the job
     * @return "Today, 4–6 PM · 12 kg"
     */
    @NonNull
    public static String slot(@NonNull Context context, @NonNull RiderJob job) {
        return context.getString(R.string.booking_slot, job.getScheduleText(), PesoFormat.number(job.getBillableKg()));
    }

    /**
     * Shows "Pickup" (blue) or "Return delivery" (orange) on a StatusBadge-styled TextView.
     *
     * @param badge the badge view
     * @param type  the job type
     */
    public static void badge(@NonNull TextView badge, @NonNull JobType type) {
        Context context = badge.getContext();
        badge.setText(type.getLabel());
        ViewCompat.setBackgroundTintList(badge,
                ColorStateList.valueOf(ContextCompat.getColor(context, type.getBadgeBackground())));
        badge.setTextColor(ContextCompat.getColor(context, type.getBadgeText()));
    }
}
