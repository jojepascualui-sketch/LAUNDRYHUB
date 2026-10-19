package com.laundryhub.admin.api.dto;

/**
 * A5 sales report.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record Report(long orderCount, java.math.BigDecimal totalSales, java.math.BigDecimal cashSales, java.math.BigDecimal gcashSales, long deliveryCount, long walkInCount, java.util.List<SoapSales> soapSales) {
}
