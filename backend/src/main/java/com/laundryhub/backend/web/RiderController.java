package com.laundryhub.backend.web;

import java.util.List;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.RiderJobService;
import com.laundryhub.backend.service.RiderService;
import com.laundryhub.backend.web.dto.AdvanceJobRequest;
import com.laundryhub.backend.web.dto.DutyRequest;
import com.laundryhub.backend.web.dto.RiderJobResponse;
import com.laundryhub.backend.web.dto.RiderLocationRequest;
import com.laundryhub.backend.web.dto.RiderSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rider app R1–R5 (role RIDER). */
@RestController
@RequestMapping("/api/rider")
@Tag(name = "Rider", description = "R1 Bookings · R2/R3 Active job · R4 Job completed · R5 Off duty")
public class RiderController {

    private final RiderService riders;
    private final RiderJobService jobs;

    /**
     * @param riders duty, GPS and counters
     * @param jobs   job dispatch
     */
    public RiderController(RiderService riders, RiderJobService jobs) {
        this.riders = riders;
        this.jobs = jobs;
    }

    /**
     * @param riderId the signed-in rider
     * @return header: name, duty and the Available / Accepted / Done today counters
     */
    @GetMapping("/summary")
    @Operation(summary = "R1 header counters")
    public RiderSummaryResponse summary(@CurrentUserId Long riderId) {
        return riders.summary(riderId);
    }

    /**
     * @param riderId the signed-in rider
     * @param request switch position
     * @return the updated header
     */
    @PatchMapping("/duty")
    @Operation(summary = "R1/R5 On duty / Off duty switch")
    public RiderSummaryResponse duty(@CurrentUserId Long riderId, @Valid @RequestBody DutyRequest request) {
        return riders.setDuty(riderId, request.onDuty());
    }

    /**
     * @param riderId the signed-in rider
     * @param request GPS fix
     */
    @PostMapping("/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "GPS fix (distance sort on R1, live map on 07)")
    public void location(@CurrentUserId Long riderId, @Valid @RequestBody RiderLocationRequest request) {
        riders.recordLocation(riderId, request);
    }

    /**
     * @param riderId the signed-in rider
     * @return open jobs, nearest first; empty when off duty
     */
    @GetMapping("/jobs/available")
    @Operation(summary = "R1 Available tab")
    public List<RiderJobResponse> available(@CurrentUserId Long riderId) {
        return jobs.available(riderId);
    }

    /**
     * @param riderId the signed-in rider
     * @return accepted and in-progress jobs
     */
    @GetMapping("/jobs/mine")
    @Operation(summary = "R1 My jobs tab")
    public List<RiderJobResponse> mine(@CurrentUserId Long riderId) {
        return jobs.mine(riderId);
    }

    /**
     * @param riderId the signed-in rider
     * @param id      job id
     * @return the job sheet
     */
    @GetMapping("/jobs/{id}")
    @Operation(summary = "R2/R3 Active job, R4 Job completed")
    public RiderJobResponse get(@CurrentUserId Long riderId, @PathVariable Long id) {
        return jobs.get(riderId, id);
    }

    /**
     * @param riderId the signed-in rider
     * @param id      job id
     * @return the accepted job; 409 if another rider was first
     */
    @PostMapping("/jobs/{id}/accept")
    @Operation(summary = "R1 Accept booking (409 if another rider took it first)")
    public RiderJobResponse accept(@CurrentUserId Long riderId, @PathVariable Long id) {
        return jobs.accept(riderId, id);
    }

    /**
     * @param riderId the signed-in rider
     * @param id      job id
     */
    @PostMapping("/jobs/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "R1 Decline (hides the job for this rider only)")
    public void decline(@CurrentUserId Long riderId, @PathVariable Long id) {
        jobs.decline(riderId, id);
    }

    /**
     * @param riderId the signed-in rider
     * @param id      job id
     * @param request optional weighed kilos for "Confirm pickup"
     * @return the job after the step
     */
    @PostMapping("/jobs/{id}/advance")
    @Operation(summary = "R2/R3 big button: complete the current step (moves the order status)")
    public RiderJobResponse advance(@CurrentUserId Long riderId, @PathVariable Long id,
                                    @Valid @RequestBody(required = false) AdvanceJobRequest request) {
        return jobs.advance(riderId, id, request == null ? null : request.weighedKg());
    }

    /**
     * @param riderId the signed-in rider
     * @param id      job id
     * @return the job after the cash step
     */
    @PostMapping("/jobs/{id}/cash-collected")
    @Operation(summary = "R3 Cash received (cash orders only)")
    public RiderJobResponse cashCollected(@CurrentUserId Long riderId, @PathVariable Long id) {
        return jobs.cashCollected(riderId, id);
    }
}
