package com.laundryhub.admin.api.dto;

/**
 * One order in the A1 table and the A2 list.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record OrderRow(Long id, String orderNo, String customerName, String customerMobile, String serviceType, String serviceName, java.math.BigDecimal weightKg, String paymentMethod, String paymentName, boolean paid, java.math.BigDecimal total, String status, String statusLabel) {
}
