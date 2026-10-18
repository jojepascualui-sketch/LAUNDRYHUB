package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.order.OrderStatus;

/**
 * A pipeline tile (A1) or filter chip (A2) with its count.
 *
 * @param status status
 * @param label  badge text
 * @param count  number of orders
 */
public record StatusCount(OrderStatus status, String label, long count) {
}
