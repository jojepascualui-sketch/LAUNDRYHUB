package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A1 Admin dashboard.
 *
 * @param ordersToday      "Orders today"
 * @param waitingToProcess "Waiting to process" (Pending orders)
 * @param kilosToday       "Kilos in today"
 * @param salesToday       "Sales today" (totals of today's orders that are not cancelled)
 * @param pipeline         "Order pipeline": one tile per status, in workflow order
 * @param recentOrders     "Recent orders" table
 */
public record DashboardResponse(long ordersToday, long waitingToProcess, BigDecimal kilosToday, BigDecimal salesToday,
                                List<StatusCount> pipeline, List<AdminOrderRow> recentOrders) {
}
