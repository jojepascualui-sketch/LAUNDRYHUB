package com.laundryhub.admin.api.dto;

/**
 * One bar of "Sales by fabric soap".
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record SoapSales(String soapName, long orders, java.math.BigDecimal kilos, java.math.BigDecimal sales) {
}
