package com.laundryhub.app.model;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;

/** Cash and GCash cards on 05 (source/Main.dc.html). The name is also the API value. */
public enum PaymentOption {

    /** Cash: green ₱ tile; the line depends on the service. */
    CASH(R.string.pay_cash, R.string.pay_cash_badge, R.color.success_bg, R.color.success_text) {
        @Override
        public int description(@Nullable ServiceType service) {
            // OOP: Polymorphism — the service decides "upon delivery" vs "at the counter".
            return (service == null ? ServiceType.WALK_IN : service).cashDescription();
        }

        @Override
        public boolean requiresMobileNumber() {
            return false;
        }
    },

    /** GCash: blue G tile; needs the customer's GCash number. */
    GCASH(R.string.pay_gcash, R.string.pay_gcash_badge, R.color.accent_tint, R.color.gcash_badge_text) {
        @Override
        public int description(@Nullable ServiceType service) {
            return R.string.pay_gcash_desc;
        }

        @Override
        public boolean requiresMobileNumber() {
            return true;
        }
    };

    // OOP: Encapsulation — fixed texts and colors per method.
    @StringRes
    private final int displayName;
    @StringRes
    private final int badge;
    @ColorRes
    private final int tileColor;
    @ColorRes
    private final int badgeColor;

    PaymentOption(@StringRes int displayName, @StringRes int badge, @ColorRes int tileColor, @ColorRes int badgeColor) {
        this.displayName = displayName;
        this.badge = badge;
        this.tileColor = tileColor;
        this.badgeColor = badgeColor;
    }

    /**
     * @param service chosen service, may be {@code null}
     * @return the grey line under the method name
     */
    // OOP: Polymorphism — each method has its own line.
    @StringRes
    public abstract int description(@Nullable ServiceType service);

    /** @return whether "GCash mobile number" is required */
    public abstract boolean requiresMobileNumber();

    /** @return "Cash" / "GCash" */
    @StringRes
    public int getDisplayName() {
        return displayName;
    }

    /** @return tile letter "₱" / "G" */
    @StringRes
    public int getBadge() {
        return badge;
    }

    /** @return tile color */
    @ColorRes
    public int getTileColor() {
        return tileColor;
    }

    /** @return tile letter color */
    @ColorRes
    public int getBadgeColor() {
        return badgeColor;
    }

    /** @return the API value */
    @NonNull
    public String apiName() {
        return name();
    }
}
