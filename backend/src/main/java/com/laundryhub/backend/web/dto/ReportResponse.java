package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A5 Sales reports for a date range (orders that are not cancelled, by order date).
 *
 * @param from          first day, inclusive
 * @param to            last day, inclusive
 * @param orderCount    number of orders
 * @param totalSales    sum of totals
 * @param cashSales     "Cash sales"
 * @param gcashSales    "GCash sales"
 * @param deliveryCount "Delivery vs walk-in" (left number)
 * @param walkInCount   "Delivery vs walk-in" (right number)
 * @param deliverySales sales of Pickup &amp; Delivery orders
 * @param walkInSales   sales of Walk-in orders
 * @param soapSales     "Sales by fabric soap" bars, highest first
 */
public record ReportResponse(LocalDate from, LocalDate to, long orderCount, BigDecimal totalSales,
                             BigDecimal cashSales, BigDecimal gcashSales, long deliveryCount, long walkInCount,
                             BigDecimal deliverySales, BigDecimal walkInSales, List<SoapSales> soapSales) {

    /**
     * One bar of "Sales by fabric soap".
     *
     * @param soapName soap
     * @param orders   number of orders
     * @param kilos    billable kilos
     * @param sales    sum of order totals
     */
    public record SoapSales(String soapName, long orders, BigDecimal kilos, BigDecimal sales) {
    }
}
