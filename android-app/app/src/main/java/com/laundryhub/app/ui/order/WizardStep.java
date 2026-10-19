package com.laundryhub.app.ui.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.laundryhub.app.R;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.ui.auth.AuthValidator;

/**
 * The four wizard steps (02–05). Each step decides when Continue is enabled, its title and its
 * screen — the same rules as {@code nextDisabled} in source/Main.dc.html.
 */
public enum WizardStep {

    /** Step 1 · Choose service: needs a service. */
    SERVICE {
        @Override
        public boolean isComplete(@NonNull OrderDraft draft) {
            return draft.getService() != null;
        }

        @Override
        public int title(@Nullable ServiceType service) {
            return R.string.step_title_service;
        }

        @NonNull
        @Override
        public Fragment createScreen() {
            return new ServiceStepFragment();
        }
    },

    /** Step 2 · Weight &amp; fabric soap: needs a soap (kilos never go below the minimum). */
    LOAD {
        @Override
        public boolean isComplete(@NonNull OrderDraft draft) {
            return draft.getSoapId() != null;
        }

        @Override
        public int title(@Nullable ServiceType service) {
            return R.string.step_title_load;
        }

        @NonNull
        @Override
        public Fragment createScreen() {
            return new LoadStepFragment();
        }
    },

    /** Step 3 · Pin / Drop-off schedule: needs a time chip; delivery also a pin that is not outside the radius. */
    SCHEDULE {
        @Override
        public boolean isComplete(@NonNull OrderDraft draft) {
            ServiceType service = draft.getService();
            boolean pinOk = draft.getPin() != null && !Boolean.FALSE.equals(draft.getPinInside());
            return draft.getSlot() != null && (service == null || !service.requiresPin() || pinOk);
        }

        @Override
        public int title(@Nullable ServiceType service) {
            // OOP: Polymorphism — the service names its own step 3.
            return (service == null ? ServiceType.WALK_IN : service).scheduleTitle();
        }

        @NonNull
        @Override
        public Fragment createScreen() {
            return new ScheduleStepFragment();
        }
    },

    /** Step 4 · Review &amp; pay: GCash needs a valid mobile number. */
    REVIEW {
        @Override
        public boolean isComplete(@NonNull OrderDraft draft) {
            return !draft.getPayment().requiresMobileNumber() || AuthValidator.isMobile(draft.getGcashNumber());
        }

        @Override
        public int title(@Nullable ServiceType service) {
            return R.string.step_title_review;
        }

        @NonNull
        @Override
        public Fragment createScreen() {
            return new ReviewStepFragment();
        }
    };

    /** Number of steps ("Step N of 4"). */
    public static final int COUNT = 4;

    /**
     * @param draft the order so far
     * @return whether Continue / Place order is enabled
     */
    // OOP: Polymorphism — each step has its own rule.
    public abstract boolean isComplete(@NonNull OrderDraft draft);

    /**
     * @param service chosen service, may be {@code null}
     * @return the step title
     */
    @StringRes
    public abstract int title(@Nullable ServiceType service);

    /** @return a new Fragment for this step */
    @NonNull
    public abstract Fragment createScreen();

    /** @return 1–4 */
    public int number() {
        return ordinal() + 1;
    }

    /** @return whether this is the last step ("Place order") */
    public boolean isLast() {
        return this == REVIEW;
    }

    /** @return the following step; the last step returns itself */
    @NonNull
    public WizardStep next() {
        return isLast() ? this : values()[ordinal() + 1];
    }

    /** @return the previous step, or {@code null} for the first */
    @Nullable
    public WizardStep previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }
}
