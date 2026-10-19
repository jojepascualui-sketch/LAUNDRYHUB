package com.laundryhub.app.model.rider;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;
import com.laundryhub.app.model.PaymentOption;

/**
 * Pickup and Return delivery jobs (R1–R4). Each type knows its badge colors and the payment lines
 * from source/Rider.dc.html ({@code payNote}, {@code doneNote}, {@code payBg}).
 */
public enum JobType {

    /** Customer → shop. Blue badge; payment happens later. */
    PICKUP(R.string.job_pickup, R.color.status_washing_bg, R.color.status_washing_text) {
        @Override
        public int paymentNote(@NonNull PaymentOption method) {
            return method == PaymentOption.CASH ? R.string.pay_note_cash_on_return : R.string.pay_note_gcash_total;
        }

        @Override
        public int doneNote(@NonNull PaymentOption method) {
            return R.string.done_note_dropped_at_shop;
        }

        @Override
        public boolean collectsCash(@NonNull PaymentOption method) {
            return false;
        }
    },

    /** Shop → customer. Orange badge; cash orders collect the money. */
    RETURN_DELIVERY(R.string.job_return_delivery, R.color.warn_bg, R.color.warn_text) {
        @Override
        public int paymentNote(@NonNull PaymentOption method) {
            return method == PaymentOption.CASH ? R.string.pay_note_collect_cash : R.string.pay_note_paid_gcash;
        }

        @Override
        public int doneNote(@NonNull PaymentOption method) {
            return method == PaymentOption.CASH ? R.string.done_note_cash_collected : R.string.pay_note_paid_gcash;
        }

        @Override
        public boolean collectsCash(@NonNull PaymentOption method) {
            return method == PaymentOption.CASH;
        }
    };

    // OOP: Encapsulation — fixed badge text and colors per type.
    @StringRes
    private final int label;
    @ColorRes
    private final int badgeBackground;
    @ColorRes
    private final int badgeText;

    JobType(@StringRes int label, @ColorRes int badgeBackground, @ColorRes int badgeText) {
        this.label = label;
        this.badgeBackground = badgeBackground;
        this.badgeText = badgeText;
    }

    /**
     * @param method how the order is paid
     * @return the payment strip text, e.g. "Collect cash"
     */
    // OOP: Polymorphism — pickup and return delivery word the payment differently.
    @StringRes
    public abstract int paymentNote(@NonNull PaymentOption method);

    /**
     * @param method how the order is paid
     * @return the R4 summary row, e.g. "Cash collected"
     */
    @StringRes
    public abstract int doneNote(@NonNull PaymentOption method);

    /**
     * @param method how the order is paid
     * @return whether the rider collects cash on this job (orange payment box on R3)
     */
    public abstract boolean collectsCash(@NonNull PaymentOption method);

    /** @return badge text "Pickup" / "Return delivery" */
    @StringRes
    public int getLabel() {
        return label;
    }

    /** @return badge background */
    @ColorRes
    public int getBadgeBackground() {
        return badgeBackground;
    }

    /** @return badge text color */
    @ColorRes
    public int getBadgeText() {
        return badgeText;
    }
}
