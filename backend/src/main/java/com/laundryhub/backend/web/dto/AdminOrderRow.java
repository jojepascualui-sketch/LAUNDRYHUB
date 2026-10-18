package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.payment.PaymentChoice;
import com.laundryhub.backend.domain.payment.PaymentMethod;

/**
 * A1 "Recent orders" row and A2 order list card.
 *
 * @param id             order id
 * @param orderNo        "LH-0001"
 * @param customerName   customer
 * @param customerMobile customer phone
 * @param serviceType    service code
 * @param serviceName    service name
 * @param weightKg       kilos
 * @param paymentMethod  CASH or GCASH
 * @param paymentName    "Cash" / "GCash"
 * @param paid           whether paid
 * @param total          total
 * @param status         status
 * @param statusLabel    badge text
 * @param createdAt      when placed
 */
public record AdminOrderRow(Long id, String orderNo, String customerName, String customerMobile,
                            ServiceCode serviceType, String serviceName, BigDecimal weightKg,
                            PaymentChoice paymentMethod, String paymentName, boolean paid, BigDecimal total,
                            OrderStatus status, String statusLabel, LocalDateTime createdAt) {

    /**
     * @param order an order
     * @return the row
     */
    public static AdminOrderRow from(Order order) {
        PaymentMethod payment = order.getPayment();
        return new AdminOrderRow(order.getId(), order.getOrderNo(), order.getCustomer().getFullName(),
                order.getCustomer().getMobileNumber(), order.getServiceCode(),
                order.getServiceType().getDisplayName(), order.getWeightKg(),
                payment == null ? null : payment.getMethod(), payment == null ? null : payment.getDisplayName(),
                payment != null && payment.isPaid(), order.getTotal(), order.getStatus(), order.getStatus().getLabel(),
                order.getCreatedAt());
    }
}
