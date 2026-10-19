package com.laundryhub.admin.api.dto;

/**
 * A2 detail panel.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record OrderDetail(OrderRow order, String soapName, Schedule schedule, @com.fasterxml.jackson.annotation.JsonProperty("pickupLocation") PinnedLocation location, PriceBreakdown price, Payment payment, java.util.List<String> statusOptions, java.util.List<Job> jobs) {
}
