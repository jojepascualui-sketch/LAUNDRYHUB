package com.laundryhub.backend.web.dto;

import java.time.LocalDateTime;

import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.OrderStatusHistory;

/**
 * One row of order_status_history.
 *
 * @param status      new status
 * @param statusLabel badge text
 * @param changedAt   when
 * @param changedBy   who (name), or {@code null} for the system
 * @param note        note, e.g. "Pickup: Dropped off at shop"
 */
public record StatusHistoryResponse(OrderStatus status, String statusLabel, LocalDateTime changedAt, String changedBy,
                                    String note) {

    /**
     * @param entry a history row
     * @return the DTO
     */
    public static StatusHistoryResponse from(OrderStatusHistory entry) {
        return new StatusHistoryResponse(entry.getStatus(), entry.getStatus().getLabel(), entry.getChangedAt(),
                entry.getChangedBy() == null ? null : entry.getChangedBy().getFullName(), entry.getNote());
    }
}
