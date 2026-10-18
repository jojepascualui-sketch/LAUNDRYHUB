package com.laundryhub.backend.web.dto;

import java.util.List;

import com.laundryhub.backend.domain.order.OrderStatus;

/**
 * A2 right panel.
 *
 * @param order          header and list fields (id, customer · phone, status badge)
 * @param soapName       "Fabric soap" tile
 * @param schedule       "Schedule" tile
 * @param pickupLocation "Pinned location" (delivery only; {@code null} shows the walk-in note)
 * @param price          price breakdown
 * @param payment        "Total · payment (paid/unpaid)"
 * @param statusOptions  "Update status" buttons valid for this service, in order
 * @param jobs           rider jobs (Assign rider)
 * @param history        status changes
 */
public record AdminOrderDetailResponse(AdminOrderRow order, String soapName, ScheduleResponse schedule,
                                       LocationResponse pickupLocation, PriceBreakdownResponse price,
                                       PaymentResponse payment, List<OrderStatus> statusOptions,
                                       List<AdminJobResponse> jobs, List<StatusHistoryResponse> history) {
}
