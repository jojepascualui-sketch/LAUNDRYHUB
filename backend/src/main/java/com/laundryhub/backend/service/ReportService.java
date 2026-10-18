package com.laundryhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.payment.PaymentChoice;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.pricing.PriceBreakdown;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.web.dto.ReportResponse;
import com.laundryhub.backend.web.dto.ReportResponse.SoapSales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5 Sales reports and "Export CSV". Sales are the totals of orders placed in the range that
 * are not cancelled (paid or not), grouped by payment method, service and soap.
 */
@Service
public class ReportService {

    /** Longest range accepted, so one request cannot load years of orders. */
    public static final int MAX_RANGE_DAYS = 366;

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> CSV_HEADER = List.of("Order", "Placed", "Customer", "Service", "Status",
            "Weight kg", "Billable kg", "Fabric soap", "Payment", "Payment status", "Laundry", "Soap add-on",
            "Delivery fee", "Total");

    private final OrderRepository orders;

    /**
     * @param orders orders
     */
    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    /**
     * @param from first day, or {@code null} for the first day of this month
     * @param to   last day, or {@code null} for today
     * @return the A5 cards and soap bars
     */
    @Transactional(readOnly = true)
    public ReportResponse report(LocalDate from, LocalDate to) {
        LocalDate start = startOrDefault(from);
        LocalDate end = endOrDefault(to);
        List<Order> sold = sales(start, end);
        Map<String, List<Order>> bySoap = new LinkedHashMap<>();
        sold.forEach(order -> bySoap.computeIfAbsent(order.getSoapName(), k -> new java.util.ArrayList<>()).add(order));
        List<SoapSales> soapSales = bySoap.entrySet().stream()
                .map(e -> new SoapSales(e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(Order::getBillableKg).reduce(BigDecimal.ZERO, BigDecimal::add),
                        sum(e.getValue(), o -> true)))
                .sorted(Comparator.comparing(SoapSales::sales).reversed())
                .toList();
        return new ReportResponse(start, end, sold.size(), sum(sold, o -> true),
                sum(sold, o -> paidWith(o, PaymentChoice.CASH)), sum(sold, o -> paidWith(o, PaymentChoice.GCASH)),
                count(sold, ServiceCode.PICKUP_DELIVERY), count(sold, ServiceCode.WALK_IN),
                sum(sold, o -> o.getServiceCode() == ServiceCode.PICKUP_DELIVERY),
                sum(sold, o -> o.getServiceCode() == ServiceCode.WALK_IN), soapSales);
    }

    /**
     * "Export CSV": one row per order in the range (cancelled orders included, marked by status).
     *
     * @param from first day, or {@code null} for the first day of this month
     * @param to   last day, or {@code null} for today
     * @return CSV text (UTF-8, comma separated, header row first)
     */
    @Transactional(readOnly = true)
    public String exportCsv(LocalDate from, LocalDate to) {
        LocalDate start = startOrDefault(from);
        LocalDate end = endOrDefault(to);
        StringBuilder csv = new StringBuilder();
        appendRow(csv, CSV_HEADER);
        for (Order order : placed(start, end)) {
            PriceBreakdown price = order.getPriceBreakdown();
            PaymentMethod payment = order.getPayment();
            appendRow(csv, List.of(order.getOrderNo(), order.getCreatedAt().format(CSV_TIME),
                    order.getCustomer().getFullName(), order.getServiceType().getDisplayName(),
                    order.getStatus().getLabel(), order.getWeightKg().toPlainString(),
                    order.getBillableKg().toPlainString(), order.getSoapName(),
                    payment == null ? "" : payment.getDisplayName(), payment == null ? "" : payment.getStatus().name(),
                    price.laundryCost().toPlainString(), price.soapCost().toPlainString(),
                    price.serviceFee().toPlainString(), price.total().toPlainString()));
        }
        return csv.toString();
    }

    /**
     * @param from first day of the range
     * @param to   last day of the range
     * @return a file name such as {@code laundryhub-sales-2026-10-01-to-2026-10-18.csv}
     */
    public String csvFileName(LocalDate from, LocalDate to) {
        return "laundryhub-sales-" + startOrDefault(from) + "-to-" + endOrDefault(to) + ".csv";
    }

    private List<Order> placed(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new IllegalArgumentException("Choose a range of at most " + MAX_RANGE_DAYS + " days");
        }
        return orders.findPlacedBetween(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private List<Order> sales(LocalDate start, LocalDate end) {
        return placed(start, end).stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
    }

    private static boolean paidWith(Order order, PaymentChoice method) {
        return order.getPayment() != null && order.getPayment().getMethod() == method;
    }

    private static BigDecimal sum(List<Order> list, Predicate<Order> filter) {
        return list.stream().filter(filter).map(Order::getTotal).reduce(Money.ZERO, BigDecimal::add);
    }

    private static long count(List<Order> list, ServiceCode service) {
        return list.stream().filter(o -> o.getServiceCode() == service).count();
    }

    private static LocalDate startOrDefault(LocalDate from) {
        return from != null ? from : LocalDate.now().withDayOfMonth(1);
    }

    private static LocalDate endOrDefault(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append("\r\n");
    }

    /** Quotes a cell when it contains a comma, quote or line break, and defuses spreadsheet formulas. */
    static String escape(String cell) {
        String value = cell == null ? "" : cell;
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 && !value.matches("-?\\d+(\\.\\d+)?")) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
