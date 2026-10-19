package com.laundryhub.admin.api.dto;

/**
 * A1 dashboard.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Dashboard(long ordersToday, long waitingToProcess, java.math.BigDecimal kilosToday, java.math.BigDecimal salesToday, java.util.List<StatusCount> pipeline, java.util.List<OrderRow> recentOrders) {
}
