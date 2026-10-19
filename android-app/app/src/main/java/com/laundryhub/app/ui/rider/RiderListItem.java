package com.laundryhub.app.ui.rider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.laundryhub.app.model.rider.RiderJob;

import java.util.Objects;

/** One row of the R1 list: a booking card, or the off-duty / empty card. */
// OOP: Abstraction — the adapter works with rows without knowing which kind they are.
public abstract class RiderListItem {

    /** View type of {@link State}. */
    public static final int TYPE_STATE = 0;
    /** View type of {@link Booking}. */
    public static final int TYPE_BOOKING = 1;

    /** @return {@link #TYPE_STATE} or {@link #TYPE_BOOKING} */
    // OOP: Polymorphism — each row kind answers its own view type, id and equality.
    public abstract int viewType();

    /** @return a stable id for RecyclerView diffing */
    public abstract long stableId();

    /**
     * @param other another row with the same id
     * @return whether nothing visible changed
     */
    public abstract boolean sameContent(@NonNull RiderListItem other);

    /** "You’re off duty" (R5) or an empty-list message. */
    // OOP: Inheritance — a RiderListItem.
    public static final class State extends RiderListItem {

        private static final long OFF_DUTY_ID = -1;
        private static final long EMPTY_ID = -2;

        // OOP: Encapsulation — immutable card content.
        private final boolean offDuty;
        @StringRes
        private final int message;

        private State(boolean offDuty, @StringRes int message) {
            this.offDuty = offDuty;
            this.message = message;
        }

        /**
         * @param message the off-duty message
         * @return the R5 card (clock, title, message)
         */
        @NonNull
        public static State offDuty(@StringRes int message) {
            return new State(true, message);
        }

        /**
         * @param message the tab's empty text
         * @return the empty-list card (message only)
         */
        @NonNull
        public static State empty(@StringRes int message) {
            return new State(false, message);
        }

        /** @return whether this is the off-duty card */
        public boolean isOffDuty() {
            return offDuty;
        }

        /** @return the message */
        @StringRes
        public int getMessage() {
            return message;
        }

        @Override
        public int viewType() {
            return TYPE_STATE;
        }

        @Override
        public long stableId() {
            return offDuty ? OFF_DUTY_ID : EMPTY_ID;
        }

        @Override
        public boolean sameContent(@NonNull RiderListItem other) {
            return other instanceof State s && s.offDuty == offDuty && s.message == message;
        }
    }

    /** A booking card: available (Decline / Accept booking) or accepted (Start / Continue job). */
    // OOP: Inheritance — a RiderListItem.
    public static final class Booking extends RiderListItem {

        // OOP: Encapsulation — immutable card content.
        private final RiderJob job;
        private final boolean accepted;
        private final boolean busy;

        /**
         * @param job      the booking
         * @param accepted whether it is on the My jobs tab
         * @param busy     whether an accept / decline call is running
         */
        public Booking(@NonNull RiderJob job, boolean accepted, boolean busy) {
            this.job = Objects.requireNonNull(job, "job");
            this.accepted = accepted;
            this.busy = busy;
        }

        /** @return the booking */
        @NonNull
        public RiderJob getJob() {
            return job;
        }

        /** @return whether it is on the My jobs tab */
        public boolean isAccepted() {
            return accepted;
        }

        /** @return whether a call is running (buttons disabled) */
        public boolean isBusy() {
            return busy;
        }

        @Override
        public int viewType() {
            return TYPE_BOOKING;
        }

        @Override
        public long stableId() {
            return job.getId();
        }

        @Override
        public boolean sameContent(@NonNull RiderListItem other) {
            return other instanceof Booking b && b.job == job && b.accepted == accepted && b.busy == busy;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof Booking b && sameContent(b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(job.getId(), accepted, busy);
        }
    }
}
