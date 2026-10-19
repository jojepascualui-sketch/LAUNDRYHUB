package com.laundryhub.app.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;
import com.laundryhub.app.ui.common.UiText;

import java.math.BigDecimal;

/**
 * The two services on 02. Each one knows its texts, icon, fee, whether a pin is needed and
 * how step 3 and the Cash card read (source/Main.dc.html).
 */
public enum ServiceType {

    /** Walk-in: no fee, drop-off schedule, "Pay at the counter". */
    WALK_IN(R.string.service_walk_in, R.string.service_walk_in_desc, R.drawable.ic_walk_in) {
        @NonNull
        @Override
        public BigDecimal fee(@NonNull Pricing pricing) {
            return BigDecimal.ZERO;
        }

        @NonNull
        @Override
        public UiText feeLabel(@NonNull Pricing pricing) {
            return UiText.of(R.string.service_walk_in_fee);
        }

        @Override
        public boolean requiresPin() {
            return false;
        }

        @Override
        public int scheduleTitle() {
            return R.string.step_title_drop_off;
        }

        @Override
        public int slotTitle() {
            return R.string.drop_off_time;
        }

        @Override
        public int cashDescription() {
            return R.string.pay_cash_counter;
        }
    },

    /** Pickup &amp; Delivery: delivery fee, pin + pickup time, "Pay the rider upon delivery". */
    PICKUP_DELIVERY(R.string.service_delivery, R.string.service_delivery_desc, R.drawable.ic_truck) {
        @NonNull
        @Override
        public BigDecimal fee(@NonNull Pricing pricing) {
            return pricing.getDeliveryFee();
        }

        @NonNull
        @Override
        public UiText feeLabel(@NonNull Pricing pricing) {
            return UiText.format(R.string.service_delivery_fee, PesoFormat.compact(pricing.getDeliveryFee()));
        }

        @Override
        public boolean requiresPin() {
            return true;
        }

        @Override
        public int scheduleTitle() {
            return R.string.step_title_pin;
        }

        @Override
        public int slotTitle() {
            return R.string.pickup_time;
        }

        @Override
        public int cashDescription() {
            return R.string.pay_cash_delivery;
        }
    };

    // OOP: Encapsulation — fixed texts and icon per service.
    @StringRes
    private final int displayName;
    @StringRes
    private final int description;
    @DrawableRes
    private final int icon;

    ServiceType(@StringRes int displayName, @StringRes int description, @DrawableRes int icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    /**
     * @param pricing current prices
     * @return the fee this service adds to the total
     */
    // OOP: Polymorphism — walk-in adds nothing, delivery adds the delivery fee.
    @NonNull
    public abstract BigDecimal fee(@NonNull Pricing pricing);

    /**
     * @param pricing current prices
     * @return the accent line on the 02 card: "No extra fee" / "+ ₱60 delivery fee"
     */
    @NonNull
    public abstract UiText feeLabel(@NonNull Pricing pricing);

    /** @return whether step 3 needs a pinned location */
    public abstract boolean requiresPin();

    /** @return step 3 title: "Pin your location" or "Drop-off schedule" */
    @StringRes
    public abstract int scheduleTitle();

    /** @return chip section title: "Pickup time" or "Drop-off time" */
    @StringRes
    public abstract int slotTitle();

    /** @return the Cash card line: "Pay the rider upon delivery" or "Pay at the counter" */
    @StringRes
    public abstract int cashDescription();

    /** @return "Walk-in" / "Pickup &amp; Delivery" */
    @StringRes
    public int getDisplayName() {
        return displayName;
    }

    /** @return the card description */
    @StringRes
    public int getDescription() {
        return description;
    }

    /** @return the icon in the card tile */
    @DrawableRes
    public int getIcon() {
        return icon;
    }
}
