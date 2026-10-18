package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.laundryhub.backend.domain.order.OrderStatus;

/**
 * 07 "Track order": the five-step timeline, the rider card and the live map.
 *
 * @param orderId       order id
 * @param orderNo       "Order #LH-0001"
 * @param status        raw status (READY has no own timeline step and shows as Washing &amp; drying)
 * @param statusLabel   badge text
 * @param cancelled     whether the order was cancelled (no design yet; no step is current then)
 * @param currentStep   index 0–4 of the current timeline step, or {@code null} if cancelled
 * @param timeline      the five design steps in order
 * @param history       every status change with time
 * @param rider         the rider handling the order now, or {@code null}
 * @param liveTracking  whether the "Rider is on the way · live GPS" chip applies
 * @param riderLocation latest rider GPS fix while the rider is on the job, else {@code null}
 * @param shop          shop marker, or {@code null} if not set
 * @param pin           customer pin, or {@code null} for walk-in
 */
public record TrackingResponse(Long orderId, String orderNo, OrderStatus status, String statusLabel, boolean cancelled,
                               Integer currentStep, List<TimelineStep> timeline, List<StatusHistoryResponse> history,
                               RiderCard rider, boolean liveTracking, RiderPosition riderLocation, Point shop,
                               Point pin) {

    /** Timeline steps in design order: Order placed, Picked up, Washing &amp; drying, Out for delivery, Delivered. */
    public enum StepKey {
        /** "Order placed". */
        ORDER_PLACED,
        /** "Picked up". */
        PICKED_UP,
        /** "Washing &amp; drying". */
        WASHING,
        /** "Out for delivery". */
        OUT_FOR_DELIVERY,
        /** "Delivered". */
        DELIVERED
    }

    /**
     * One timeline row.
     *
     * @param key     which step
     * @param done    filled accent dot with check
     * @param current accent ring
     */
    public record TimelineStep(StepKey key, boolean done, boolean current) {
    }

    /**
     * Rider card: initials, "[RIDER NAME]", "Your rider · [PLATE NO.]", call button.
     *
     * @param name         full name
     * @param initials     avatar initials
     * @param plateNumber  plate
     * @param mobileNumber number for the call button, or {@code null}
     */
    public record RiderCard(String name, String initials, String plateNumber, String mobileNumber) {
    }

    /**
     * A rider GPS fix.
     *
     * @param latitude   degrees
     * @param longitude  degrees
     * @param recordedAt when the phone sent it
     */
    public record RiderPosition(BigDecimal latitude, BigDecimal longitude, LocalDateTime recordedAt) {
    }

    /**
     * A map marker.
     *
     * @param latitude  degrees
     * @param longitude degrees
     */
    public record Point(BigDecimal latitude, BigDecimal longitude) {
    }
}
