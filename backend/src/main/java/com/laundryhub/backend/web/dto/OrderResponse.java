package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.ServiceCode;

/**
 * One order in full: 06 "Order placed" summary and the customer's order detail.
 *
 * @param id              order id
 * @param orderNo         "LH-0001"
 * @param status          status
 * @param statusLabel     badge text, e.g. "Out for delivery"
 * @param serviceType     service code
 * @param serviceName     service name
 * @param weightKg        kilos (weighed kilos after pickup)
 * @param billableKg      kilos charged
 * @param soapName        "Fabric soap"
 * @param schedule        "Schedule"
 * @param pickupLocation  pin (delivery only), else {@code null}
 * @param payment         "Payment"
 * @param price           server-calculated breakdown, "Total"
 * @param cancelledReason reason, if cancelled
 * @param createdAt       when it was placed
 */
public record OrderResponse(Long id, String orderNo, OrderStatus status, String statusLabel, ServiceCode serviceType,
                            String serviceName, BigDecimal weightKg, BigDecimal billableKg, String soapName,
                            ScheduleResponse schedule, LocationResponse pickupLocation, PaymentResponse payment,
                            PriceBreakdownResponse price, String cancelledReason, LocalDateTime createdAt) {

    /**
     * @param order an order (inside a transaction)
     * @param today current date for "Today"/"Tomorrow"
     * @return the DTO
     */
    public static OrderResponse from(Order order, LocalDate today) {
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getStatus(), order.getStatus().getLabel(),
                order.getServiceCode(), order.getServiceType().getDisplayName(), order.getWeightKg(),
                order.getBillableKg(), order.getSoapName(), ScheduleResponse.from(order.getSchedule(), today),
                LocationResponse.from(order.getPickupLocation(), order.getPickupGeocodedAddress()),
                PaymentResponse.from(order.getPayment()), PriceBreakdownResponse.from(order.getPriceBreakdown()),
                order.getCancelledReason(), order.getCreatedAt());
    }
}
