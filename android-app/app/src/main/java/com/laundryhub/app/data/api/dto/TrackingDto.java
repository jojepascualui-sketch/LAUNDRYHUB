package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** {@code GET /api/orders/{id}/tracking} (07). Filled by Gson. */
public final class TrackingDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long orderId;
    private String orderNo;
    private String statusLabel;
    private boolean cancelled;
    private List<Step> timeline;
    private Rider rider;
    private boolean liveTracking;
    private Point riderLocation;
    private Point shop;
    private Point pin;

    /** For Gson. */
    public TrackingDto() {
    }

    /** @return order id */
    public Long getOrderId() {
        return orderId;
    }

    /** @return order number */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return status text */
    public String getStatusLabel() {
        return statusLabel;
    }

    /** @return whether the order was cancelled */
    public boolean isCancelled() {
        return cancelled;
    }

    /** @return the five design steps */
    public List<Step> getTimeline() {
        return timeline == null ? Collections.emptyList() : timeline;
    }

    /** @return the rider card, or {@code null} */
    public Rider getRider() {
        return rider;
    }

    /** @return whether the rider shares live GPS */
    public boolean isLiveTracking() {
        return liveTracking;
    }

    /** @return the rider's last GPS fix, or {@code null} */
    public Point getRiderLocation() {
        return riderLocation;
    }

    /** @return the shop, or {@code null} */
    public Point getShop() {
        return shop;
    }

    /** @return the customer's pin, or {@code null} (walk-in) */
    public Point getPin() {
        return pin;
    }

    /** A map point. */
    public static final class Point {
        private BigDecimal latitude;
        private BigDecimal longitude;

        /** @return latitude */
        public BigDecimal getLatitude() {
            return latitude;
        }

        /** @return longitude */
        public BigDecimal getLongitude() {
            return longitude;
        }
    }

    /** One timeline step. */
    public static final class Step {
        private String key;
        private boolean done;
        private boolean current;

        /** @return ORDER_PLACED, PICKED_UP, WASHING, OUT_FOR_DELIVERY or DELIVERED */
        public String getKey() {
            return key;
        }

        /** @return whether it is done */
        public boolean isDone() {
            return done;
        }

        /** @return whether it is the current step */
        public boolean isCurrent() {
            return current;
        }
    }

    /** "Your rider" card. */
    public static final class Rider {
        private String name;
        private String initials;
        private String plateNumber;
        private String mobileNumber;

        /** @return rider name */
        public String getName() {
            return name;
        }

        /** @return avatar initials */
        public String getInitials() {
            return initials;
        }

        /** @return plate number */
        public String getPlateNumber() {
            return plateNumber;
        }

        /** @return phone, or {@code null} */
        public String getMobileNumber() {
            return mobileNumber;
        }
    }
}
