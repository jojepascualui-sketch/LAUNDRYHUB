package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.laundryhub.backend.domain.payment.PaymentChoice;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.JobType;

/**
 * A booking card (R1) and the active job sheet (R2/R3/R4).
 *
 * @param id                 job id
 * @param orderId            order id
 * @param orderNo            "LH-0003"
 * @param type               PICKUP or RETURN_DELIVERY
 * @param typeLabel          badge text "Pickup" / "Return delivery"
 * @param status             job status
 * @param distanceKm         "· 1.2 km" from the rider's last GPS fix to the first stop, or {@code null}
 * @param customerName       customer
 * @param customerInitials   avatar initials
 * @param customerMobile     call button; only for the assigned rider
 * @param customerPin        customer pin and address
 * @param shop               shop point (start of a return delivery), or {@code null}
 * @param navigationUrl      "Navigate in Google Maps" link to the next stop, or {@code null}
 * @param scheduleText       "Today, 4–6 PM"
 * @param billableKg         kilos ("· 12 kg")
 * @param total              order total
 * @param payment            payment strip
 * @param steps              checklist
 * @param currentStepIndex   index of the next step (equals steps done)
 * @param currentActionLabel big button text, or {@code null} when completed
 * @param awaitingCash       whether the next step is "Cash collected" (use the cash-collected endpoint)
 * @param acceptedAt         when accepted
 * @param completedAt        when completed (R4)
 */
public record RiderJobResponse(Long id, Long orderId, String orderNo, JobType type, String typeLabel, JobStatus status,
                               BigDecimal distanceKm, String customerName, String customerInitials,
                               String customerMobile, LocationResponse customerPin, TrackingResponse.Point shop,
                               String navigationUrl, String scheduleText, BigDecimal billableKg, BigDecimal total,
                               PaymentStrip payment, List<Step> steps, int currentStepIndex,
                               String currentActionLabel, boolean awaitingCash, LocalDateTime acceptedAt,
                               LocalDateTime completedAt) {

    /**
     * R1 payment strip ("Cash on return · order total" / "Collect cash" / "Paid via GCash").
     *
     * @param method      CASH or GCASH
     * @param paid        whether it is paid
     * @param collectCash whether this job collects cash from the customer
     * @param amountDue   still owed
     */
    public record PaymentStrip(PaymentChoice method, boolean paid, boolean collectCash, BigDecimal amountDue) {
    }

    /**
     * One checklist row.
     *
     * @param title       e.g. "Arrived at pickup point"
     * @param actionLabel button text while current, e.g. "I’ve arrived"
     * @param done        filled dot
     * @param current     accent ring, bold text
     */
    public record Step(String title, String actionLabel, boolean done, boolean current) {
    }
}
