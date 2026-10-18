package com.laundryhub.backend.service;

import java.util.ArrayList;
import java.util.List;

import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.JobType;
import com.laundryhub.backend.domain.rider.PickupJob;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.RiderLocationRepository;
import com.laundryhub.backend.web.dto.StatusHistoryResponse;
import com.laundryhub.backend.web.dto.TrackingResponse;
import com.laundryhub.backend.web.dto.TrackingResponse.Point;
import com.laundryhub.backend.web.dto.TrackingResponse.RiderCard;
import com.laundryhub.backend.web.dto.TrackingResponse.RiderPosition;
import com.laundryhub.backend.web.dto.TrackingResponse.StepKey;
import com.laundryhub.backend.web.dto.TrackingResponse.TimelineStep;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 07 "Track order". Order statuses are mapped onto the five design steps (there is no "Ready"
 * step: Ready stays on "Washing &amp; drying"). The rider's GPS is only shared while that rider is
 * working on this order.
 */
@Service
public class TrackingService {

    private static final int STEP_PLACED = StepKey.ORDER_PLACED.ordinal();
    private static final int STEP_PICKED_UP = StepKey.PICKED_UP.ordinal();
    private static final int STEP_WASHING = StepKey.WASHING.ordinal();
    private static final int STEP_OUT = StepKey.OUT_FOR_DELIVERY.ordinal();
    private static final int STEP_DELIVERED = StepKey.DELIVERED.ordinal();

    private final OrderService orderService;
    private final AccountLookup accounts;
    private final RiderJobRepository jobs;
    private final RiderLocationRepository locations;
    private final DeliveryZoneService zone;

    /**
     * @param orderService loads the customer's own order
     * @param accounts     signed-in account lookup
     * @param jobs         rider jobs
     * @param locations    rider GPS fixes
     * @param zone         shop position
     */
    public TrackingService(OrderService orderService, AccountLookup accounts, RiderJobRepository jobs,
                           RiderLocationRepository locations, DeliveryZoneService zone) {
        this.orderService = orderService;
        this.accounts = accounts;
        this.jobs = jobs;
        this.locations = locations;
        this.zone = zone;
    }

    /**
     * @param customerId the signed-in customer
     * @param orderId    one of their orders
     * @return timeline, history, rider card and latest rider location
     */
    @Transactional(readOnly = true)
    public TrackingResponse track(Long customerId, Long orderId) {
        accounts.customer(customerId);
        Order order = orderService.findOwned(customerId, orderId);
        List<RiderJob> orderJobs = jobs.findByOrderId(order.getId());
        RiderJob pickup = ofType(orderJobs, JobType.PICKUP);
        RiderJob delivery = ofType(orderJobs, JobType.RETURN_DELIVERY);

        Integer current = currentStep(order.getStatus(), pickup);
        List<TimelineStep> timeline = new ArrayList<>();
        for (StepKey key : StepKey.values()) {
            int i = key.ordinal();
            boolean done = current != null && (i < current || (i == current && current == STEP_DELIVERED));
            timeline.add(new TimelineStep(key, done, current != null && i == current && !done));
        }

        RiderJob riderJob = jobInCharge(pickup, delivery);
        Rider rider = riderJob == null ? null : riderJob.getRider();
        boolean live = riderJob != null && riderJob.isActive();
        RiderPosition position = live
                ? locations.findFirstByRiderOrderByRecordedAtDesc(rider)
                        .map(fix -> new RiderPosition(fix.getPosition().getLatitude(), fix.getPosition().getLongitude(),
                                fix.getRecordedAt()))
                        .orElse(null)
                : null;
        RiderCard card = rider == null ? null
                : new RiderCard(rider.getFullName(), rider.getInitials(), rider.getPlateNumber(), rider.getMobileNumber());

        List<StatusHistoryResponse> history = order.getStatusHistory().stream().map(StatusHistoryResponse::from).toList();
        return new TrackingResponse(order.getId(), order.getOrderNo(), order.getStatus(), order.getStatus().getLabel(),
                order.getStatus() == OrderStatus.CANCELLED, current, timeline, history, card, live, position,
                point(zone.findShopLocation()), point(order.getPickupLocation()));
    }

    /**
     * Maps an order status onto the design's five steps.
     *
     * @param status the order status
     * @param pickup the pickup job, or {@code null} for walk-in
     * @return the current step index, or {@code null} for a cancelled order
     */
    static Integer currentStep(OrderStatus status, RiderJob pickup) {
        return switch (status) {
            case PENDING -> pickup instanceof PickupJob p && p.isLaundryCollected() ? STEP_PICKED_UP : STEP_PLACED;
            case WASHING, READY -> STEP_WASHING;
            case OUT_FOR_DELIVERY -> STEP_OUT;
            case COMPLETED -> STEP_DELIVERED;
            case CANCELLED -> null;
        };
    }

    /** The return-delivery rider once that job is under way, otherwise the pickup rider. */
    private static RiderJob jobInCharge(RiderJob pickup, RiderJob delivery) {
        if (delivery != null && delivery.getRider() != null && delivery.getStatus() != JobStatus.WAITING) {
            return delivery;
        }
        return pickup != null && pickup.getRider() != null ? pickup : null;
    }

    private static RiderJob ofType(List<RiderJob> jobs, JobType type) {
        return jobs.stream().filter(job -> job.getType() == type).findFirst().orElse(null);
    }

    private static Point point(Location location) {
        return location == null ? null : new Point(location.getLatitude(), location.getLongitude());
    }
}
