package com.laundryhub.backend.service;

import java.time.LocalDate;
import java.util.List;

import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.RiderLocation;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.RiderLocationRepository;
import com.laundryhub.backend.web.dto.RiderLocationRequest;
import com.laundryhub.backend.web.dto.RiderSummaryResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** R1/R5 header: duty switch, GPS updates and the three counters. */
@Service
public class RiderService {

    /** Job statuses counted as "Accepted" (and listed under "My jobs"). */
    public static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.ACCEPTED, JobStatus.IN_PROGRESS);

    private final AccountLookup accounts;
    private final RiderJobRepository jobs;
    private final RiderLocationRepository locations;

    /**
     * @param accounts  signed-in account lookup
     * @param jobs      rider jobs
     * @param locations GPS fixes
     */
    public RiderService(AccountLookup accounts, RiderJobRepository jobs, RiderLocationRepository locations) {
        this.accounts = accounts;
        this.jobs = jobs;
        this.locations = locations;
    }

    /**
     * The On duty / Off duty switch. Off duty hides the bookings list (R5).
     *
     * @param riderId the signed-in rider
     * @param onDuty  new position
     * @return the updated header
     */
    @Transactional
    public RiderSummaryResponse setDuty(Long riderId, boolean onDuty) {
        Rider rider = accounts.rider(riderId);
        if (onDuty) {
            rider.goOnDuty();
        } else {
            rider.goOffDuty();
        }
        return summaryOf(rider);
    }

    /**
     * Stores a GPS fix: sorts "Available" by distance and feeds the live map on 07.
     *
     * @param riderId the signed-in rider
     * @param request the fix
     */
    @Transactional
    public void recordLocation(Long riderId, RiderLocationRequest request) {
        Rider rider = accounts.rider(riderId);
        locations.save(new RiderLocation(rider, new Location(request.lat(), request.lng(), null),
                request.accuracyMeters()));
    }

    /**
     * @param riderId the signed-in rider
     * @return name, duty and the counters Available / Accepted / Done today
     */
    @Transactional(readOnly = true)
    public RiderSummaryResponse summary(Long riderId) {
        return summaryOf(accounts.rider(riderId));
    }

    private RiderSummaryResponse summaryOf(Rider rider) {
        long available = jobs.findOpenFor(rider, JobStatus.AVAILABLE).size();
        long accepted = jobs.countByRiderAndStatusIn(rider, ACTIVE_STATUSES);
        long doneToday = jobs.countByRiderAndStatusAndCompletedAtGreaterThanEqual(rider, JobStatus.COMPLETED,
                LocalDate.now().atStartOfDay());
        return new RiderSummaryResponse(rider.getFullName(), rider.getInitials(), rider.getPlateNumber(),
                rider.isOnDuty(), available, accepted, doneToday);
    }

    /**
     * @param rider a rider
     * @return the rider's latest GPS position, or {@code null}
     */
    @Transactional(readOnly = true)
    public Location lastPosition(Rider rider) {
        return locations.findFirstByRiderOrderByRecordedAtDesc(rider).map(RiderLocation::getPosition).orElse(null);
    }
}
