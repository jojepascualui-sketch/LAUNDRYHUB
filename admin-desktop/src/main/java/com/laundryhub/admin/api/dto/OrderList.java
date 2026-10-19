package com.laundryhub.admin.api.dto;

/**
 * A2 order list with counts per status.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record OrderList(long totalCount, java.util.List<StatusCount> counts, java.util.List<OrderRow> orders) {
}
