package com.laundryhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.rider.DeliveryJob;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.JobStep;
import com.laundryhub.backend.domain.rider.PickupJob;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.RiderJobDeclineRepository;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.web.dto.LocationResponse;
import com.laundryhub.backend.web.dto.RiderJobResponse;
import com.laundryhub.backend.web.dto.RiderJobResponse.PaymentStrip;
import com.laundryhub.backend.web.dto.RiderJobResponse.Step;
import com.laundryhub.backend.web.dto.TrackingResponse.Point;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rider booking dispatch (R1–R4): list, accept, decline and work through job steps.
 * Accepting is protected by optimistic locking ({@code @Version} on rider_jobs): when two riders
 * tap "Accept booking" at the same time, only the first commit wins and the other gets 409.
 */
@Service
public class RiderJobService {

    /** Message for the rider who loses the race for a booking. */
    public static final String ALREADY_TAKEN = "This booking was already taken by another rider.";

    private final AccountLookup accounts;
    private final RiderJobRepository jobs;
    private final RiderJobDeclineRepository declines;
    private final PricingSettingsRepository pricing;
    private final RiderService riders;
    private final DeliveryZoneService zone;

    /**
     * @param accounts signed-in account lookup
     * @param jobs     rider jobs
     * @param declines per-rider declines
     * @param pricing  minimum load for weighing
     * @param riders   last GPS position
     * @param zone     shop position
     */
    public RiderJobService(AccountLookup accounts, RiderJobRepository jobs, RiderJobDeclineRepository declines,
                           PricingSettingsRepository pricing, RiderService riders, DeliveryZoneService zone) {
        this.accounts = accounts;
        this.jobs = jobs;
        this.declines = declines;
        this.pricing = pricing;
        this.riders = riders;
        this.zone = zone;
    }

    /**
     * R1 "Available": open jobs this rider has not declined, nearest first (from the rider's last
     * GPS fix to the job's first stop). Off-duty riders get an empty list (R5).
     *
     * @param riderId the signed-in rider
     * @return booking cards
     */
    @Transactional(readOnly = true)
    public List<RiderJobResponse> available(Long riderId) {
        Rider rider = accounts.rider(riderId);
        if (!rider.isOnDuty()) {
            return List.of();
        }
        Location here = riders.lastPosition(rider);
        Location shop = zone.findShopLocation();
        List<RiderJobResponse> cards = new ArrayList<>();
        for (RiderJob job : jobs.findOpenFor(rider, JobStatus.AVAILABLE)) {
            cards.add(toResponse(job, here, shop, false));
        }
        // Stable sort: jobs without a distance keep their oldest-first order at the end.
        cards.sort(Comparator.comparing(RiderJobResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        return cards;
    }

    /**
     * R1 "My jobs": accepted and in-progress jobs.
     *
     * @param riderId the signed-in rider
     * @return the rider's active jobs, newest first
     */
    @Transactional(readOnly = true)
    public List<RiderJobResponse> mine(Long riderId) {
        Rider rider = accounts.rider(riderId);
        Location here = riders.lastPosition(rider);
        Location shop = zone.findShopLocation();
        return jobs.findByRiderAndStatusInOrderByAcceptedAtDesc(rider, RiderService.ACTIVE_STATUSES).stream()
                .map(job -> toResponse(job, here, shop, true))
                .toList();
    }

    /**
     * R2/R3/R4: one job, if it is the rider's or still available.
     *
     * @param riderId the signed-in rider
     * @param jobId   job id
     * @return the job sheet
     */
    @Transactional(readOnly = true)
    public RiderJobResponse get(Long riderId, Long jobId) {
        Rider rider = accounts.rider(riderId);
        RiderJob job = jobs.findById(jobId).orElseThrow(RiderJobService::notFound);
        boolean mine = job.isAssignedTo(rider);
        if (!mine && job.getStatus() != JobStatus.AVAILABLE) {
            throw notFound();
        }
        return toResponse(job, riders.lastPosition(rider), zone.findShopLocation(), mine);
    }

    /**
     * "Accept booking". Only one rider can win: a stale version on save means someone else was faster.
     *
     * @param riderId the signed-in rider
     * @param jobId   job id
     * @return the accepted job (opens R2/R3)
     * @throws ConflictException if the job is taken or the rider is off duty
     */
    @Transactional
    public RiderJobResponse accept(Long riderId, Long jobId) {
        Rider rider = accounts.rider(riderId);
        RiderJob job = jobs.findById(jobId).orElseThrow(RiderJobService::notFound);
        if (!rider.isOnDuty()) {
            throw new ConflictException("Go on duty to accept bookings.");
        }
        if (job.getStatus() != JobStatus.AVAILABLE) {
            throw new ConflictException(ALREADY_TAKEN);
        }
        job.accept(rider);
        try {
            jobs.saveAndFlush(job);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException(ALREADY_TAKEN);
        }
        return toResponse(job, riders.lastPosition(rider), zone.findShopLocation(), true);
    }

    /**
     * "Decline": hides the job from this rider only; other riders still see it.
     *
     * @param riderId the signed-in rider
     * @param jobId   job id
     */
    @Transactional
    public void decline(Long riderId, Long jobId) {
        Rider rider = accounts.rider(riderId);
        RiderJob job = jobs.findById(jobId).orElseThrow(RiderJobService::notFound);
        if (job.getStatus() != JobStatus.AVAILABLE) {
            throw new ConflictException("This booking is no longer available.");
        }
        declines.save(job.decline(rider));
    }

    /**
     * The big button on R2/R3: completes the current step. Steps move the order automatically
     * (pickup done → Washing; return delivery started → Out for delivery; delivered → Completed)
     * and each change is written to order_status_history.
     *
     * @param riderId   the signed-in rider
     * @param jobId     job id
     * @param weighedKg optional kilos on the scale, only with "Confirm pickup"
     * @return the updated job; status COMPLETED means R4
     * @throws ConflictException if the next step is "Cash collected" (use {@link #cashCollected})
     */
    @Transactional
    public RiderJobResponse advance(Long riderId, Long jobId, BigDecimal weighedKg) {
        Rider rider = accounts.rider(riderId);
        RiderJob job = assigned(riderId, jobId);
        if (job.isAwaitingCash()) {
            throw new ConflictException("Confirm the cash first with \"Cash received\".");
        }
        if (weighedKg != null) {
            if (!(job instanceof PickupJob pickup) || !pickup.isAtWeighingStep()) {
                throw new IllegalArgumentException("The weight can only be sent with \"Confirm pickup\".");
            }
            Order order = job.getOrder();
            order.recordWeighedLoad(LaundryLoad.measured(weighedKg, pricing.getSettings().getMinKg()));
        }
        job.completeCurrentStep(rider);
        return saved(job, rider);
    }

    /**
     * "Cash received" on a cash return delivery: completes the "Cash collected" step and marks
     * the order's cash payment as paid, collected by this rider.
     *
     * @param riderId the signed-in rider
     * @param jobId   job id
     * @return the updated job
     * @throws ConflictException if the job is not waiting for cash
     */
    @Transactional
    public RiderJobResponse cashCollected(Long riderId, Long jobId) {
        Rider rider = accounts.rider(riderId);
        RiderJob job = assigned(riderId, jobId);
        if (!(job instanceof DeliveryJob delivery) || !delivery.isAwaitingCash()) {
            throw new ConflictException("This job is not waiting for cash.");
        }
        delivery.confirmCashCollected(rider);
        return saved(job, rider);
    }

    private RiderJobResponse saved(RiderJob job, Rider rider) {
        try {
            jobs.saveAndFlush(job);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("This job was changed at the same time. Please refresh.");
        }
        return toResponse(job, riders.lastPosition(rider), zone.findShopLocation(), true);
    }

    private RiderJob assigned(Long riderId, Long jobId) {
        return jobs.findByIdAndRiderId(jobId, riderId).orElseThrow(RiderJobService::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Job not found");
    }

    /**
     * Builds a booking card / job sheet.
     *
     * @param job         the job
     * @param riderHere   rider's last position, or {@code null}
     * @param shop        shop position, or {@code null}
     * @param showContact whether to include the customer's phone (assigned rider only)
     */
    private static RiderJobResponse toResponse(RiderJob job, Location riderHere, Location shop, boolean showContact) {
        Order order = job.getOrder();
        // OOP: Polymorphism — PickupJob starts at the customer, DeliveryJob at the shop.
        Location start = job.getStartPoint(shop);
        BigDecimal distance = riderHere == null || start == null
                ? null : DeliveryZoneService.roundKm(riderHere.distanceKmTo(start));
        Location next = nextStop(job, order, shop);

        List<JobStep> steps = job.getSteps();
        int index = job.getCurrentStepIndex();
        List<Step> checklist = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            checklist.add(new Step(steps.get(i).title(), steps.get(i).actionLabel(), i < index, i == index));
        }
        PaymentMethod payment = order.getPayment();
        PaymentStrip strip = payment == null ? null
                : new PaymentStrip(payment.getMethod(), payment.isPaid(),
                        job instanceof DeliveryJob && payment.collectsCashOnDelivery() && !payment.isPaid(),
                        payment.getAmountDue());
        JobStep current = job.getCurrentStep();
        return new RiderJobResponse(job.getId(), order.getId(), order.getOrderNo(), job.getType(),
                job.getType().getLabel(), job.getStatus(), distance, order.getCustomer().getFullName(),
                order.getCustomer().getInitials(), showContact ? order.getCustomer().getMobileNumber() : null,
                LocationResponse.from(order.getPickupLocation(), order.getPickupGeocodedAddress()),
                shop == null ? null : new Point(shop.getLatitude(), shop.getLongitude()),
                next == null ? null : LocationResponse.MAPS_DIRECTIONS_URL + LocationResponse.coordinates(next),
                order.getSchedule().displayText(LocalDate.now()), order.getBillableKg(), order.getTotal(), strip,
                checklist, index, current == null ? null : current.actionLabel(), job.isAwaitingCash(),
                job.getAcceptedAt(), job.getCompletedAt());
    }

    /** Where "Navigate in Google Maps" should lead: the first stop until the rider has left it. */
    private static Location nextStop(RiderJob job, Order order, Location shop) {
        if (job.getStatus() == JobStatus.COMPLETED) {
            return null;
        }
        Location customer = order.getPickupLocation();
        if (job instanceof PickupJob pickup) {
            return pickup.isLaundryCollected() ? shop : customer;
        }
        return job.getCurrentStepIndex() == 0 ? shop : customer;
    }
}
