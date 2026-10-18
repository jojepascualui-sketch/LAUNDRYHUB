package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.rider.JobType;

import jakarta.validation.constraints.Size;

/**
 * A2 "Update status" and "Assign rider". Send either or both.
 *
 * @param status       new status, or {@code null} to keep it
 * @param cancelReason required when {@code status} is CANCELLED
 * @param riderId      rider to assign or reassign, or {@code null}
 * @param jobType      which job gets the rider; {@code null} = the next unfinished job
 */
public record AdminOrderUpdateRequest(OrderStatus status, @Size(max = 255) String cancelReason, Long riderId,
                                      JobType jobType) {
}
