package com.laundryhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.web.dto.AdminOrderRow;
import com.laundryhub.backend.web.dto.DashboardResponse;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A1 Admin dashboard: today's numbers, the order pipeline and recent orders. */
@Service
public class DashboardService {

    /** Rows in "Recent orders" (the rest is behind "View all"). */
    public static final int RECENT_ORDERS = 6;

    private final OrderRepository orders;
    private final AdminOrderService adminOrders;

    /**
     * @param orders      orders
     * @param adminOrders status counts
     */
    public DashboardService(OrderRepository orders, AdminOrderService adminOrders) {
        this.orders = orders;
        this.adminOrders = adminOrders;
    }

    /** @return the dashboard; "today" uses the shop time zone */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        List<Order> placedToday = orders.findPlacedBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<Order> counted = placedToday.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        BigDecimal kilos = counted.stream().map(Order::getWeightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sales = counted.stream().map(Order::getTotal).reduce(Money.ZERO, BigDecimal::add);
        List<AdminOrderRow> recent = orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_ORDERS)).stream()
                .map(AdminOrderRow::from)
                .toList();
        return new DashboardResponse(placedToday.size(), orders.countByStatus(OrderStatus.PENDING), kilos, sales,
                adminOrders.countPerStatus(), recent);
    }
}
