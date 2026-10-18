package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.ServiceCode;

/**
 * A row in the customer's Orders tab (opens 07 Track order).
 *
 * @param id           order id
 * @param orderNo      "LH-0001"
 * @param status       status
 * @param statusLabel  badge text
 * @param serviceType  service code
 * @param serviceName  service name
 * @param weightKg     kilos
 * @param total        total
 * @param scheduleText e.g. "Today, 2–4 PM"
 * @param createdAt    when it was placed
 */
public record OrderSummaryResponse(Long id, String orderNo, OrderStatus status, String statusLabel,
                                   ServiceCode serviceType, String serviceName, BigDecimal weightKg, BigDecimal total,
                                   String scheduleText, LocalDateTime createdAt) {

    /**
     * @param order an order
     * @param today current date
     * @return the DTO
     */
    public static OrderSummaryResponse from(Order order, LocalDate today) {
        return new OrderSummaryResponse(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getStatus().getLabel(), order.getServiceCode(), order.getServiceType().getDisplayName(),
                order.getWeightKg(), order.getTotal(), order.getSchedule().displayText(today), order.getCreatedAt());
    }
}
