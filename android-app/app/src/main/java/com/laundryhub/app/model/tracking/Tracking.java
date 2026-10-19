package com.laundryhub.app.model.tracking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.model.Location;

import java.util.List;

/** Everything on 07 Track order. Immutable. */
public final class Tracking {

    /** How far a timeline step is. */
    public enum State {
        /** Accent dot with a check. */
        DONE,
        /** Accent ring. */
        CURRENT,
        /** Grey ring, grey title. */
        UPCOMING
    }

    /**
     * One timeline row.
     *
     * @param step  which step
     * @param state how far it is
     */
    public record Row(@NonNull TrackStep step, @NonNull State state) {
        /** Validates the row. */
        public Row {
            if (step == null || state == null) {
                throw new IllegalArgumentException("Step and state are required");
            }
        }
    }

    /**
     * The "Your rider" card.
     *
     * @param name     rider name
     * @param initials avatar text
     * @param plate    plate number, may be empty
     * @param mobile   phone, or {@code null}
     */
    public record Rider(@NonNull String name, @NonNull String initials, @NonNull String plate, @Nullable String mobile) {
        /** Validates the name. */
        public Rider {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Rider name is required");
            }
        }
    }

    // OOP: Encapsulation — private final fields, validated in the constructor.
    private final long orderId;
    private final String orderNo;
    private final String statusLabel;
    private final boolean cancelled;
    private final List<Row> rows;
    @Nullable
    private final Rider rider;
    private final boolean live;
    @Nullable
    private final Location riderPosition;
    @Nullable
    private final Location shop;
    @Nullable
    private final Location pin;

    /**
     * @param orderId       order id
     * @param orderNo       "LH-0001"
     * @param statusLabel   server status text
     * @param cancelled     whether the order was cancelled
     * @param rows          the five timeline rows
     * @param rider         rider card, or {@code null} before a rider is assigned
     * @param live          whether the rider shares live GPS
     * @param riderPosition last GPS fix, or {@code null}
     * @param shop          shop position, or {@code null}
     * @param pin           customer pin, or {@code null} (walk-in)
     * @throws IllegalArgumentException if the order number or rows are missing
     */
    public Tracking(long orderId, @NonNull String orderNo, @NonNull String statusLabel, boolean cancelled,
                    @NonNull List<Row> rows, @Nullable Rider rider, boolean live, @Nullable Location riderPosition,
                    @Nullable Location shop, @Nullable Location pin) {
        if (orderNo == null || orderNo.isBlank() || rows == null) {
            throw new IllegalArgumentException("Order number and timeline are required");
        }
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
        this.cancelled = cancelled;
        this.rows = List.copyOf(rows);
        this.rider = rider;
        this.live = live;
        this.riderPosition = riderPosition;
        this.shop = shop;
        this.pin = pin;
    }

    /** @return order id */
    public long getOrderId() {
        return orderId;
    }

    /** @return "LH-0001" */
    @NonNull
    public String getOrderNo() {
        return orderNo;
    }

    /** @return server status text */
    @NonNull
    public String getStatusLabel() {
        return statusLabel;
    }

    /** @return whether the order was cancelled */
    public boolean isCancelled() {
        return cancelled;
    }

    /** @return timeline rows (read-only) */
    @NonNull
    public List<Row> getRows() {
        return rows;
    }

    /** @return rider card, or {@code null} */
    @Nullable
    public Rider getRider() {
        return rider;
    }

    /** @return whether "Rider is on the way · live GPS" applies */
    public boolean isLive() {
        return live && riderPosition != null;
    }

    /** @return last rider position, or {@code null} */
    @Nullable
    public Location getRiderPosition() {
        return riderPosition;
    }

    /** @return shop, or {@code null} */
    @Nullable
    public Location getShop() {
        return shop;
    }

    /** @return whether the last step ("Delivered") is done, so the order can be rated */
    public boolean isCompleted() {
        return !rows.isEmpty() && rows.get(rows.size() - 1).state() == State.DONE;
    }

    /** @return customer pin, or {@code null} */
    @Nullable
    public Location getPin() {
        return pin;
    }
}
