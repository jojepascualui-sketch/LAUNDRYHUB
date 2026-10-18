package com.laundryhub.backend.web.dto;

import java.util.List;

/**
 * A2 left column: filter chips with counts and the order cards.
 *
 * @param totalCount "All (n)" chip
 * @param counts     one chip per status
 * @param orders     orders matching the filter and search
 */
public record AdminOrderListResponse(long totalCount, List<StatusCount> counts, List<AdminOrderRow> orders) {
}
