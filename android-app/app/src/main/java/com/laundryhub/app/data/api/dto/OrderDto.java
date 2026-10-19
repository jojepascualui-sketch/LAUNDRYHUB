package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** The parts of an order response used on 06. Filled by Gson. */
public final class OrderDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long id;
    private String orderNo;
    private String serviceName;
    private BigDecimal weightKg;
    private String soapName;
    private Schedule schedule;
    private Payment payment;
    private PriceBreakdownDto price;

    /** For Gson. */
    public OrderDto() {
    }

    /** @return order id */
    public Long getId() {
        return id;
    }

    /** @return "LH-0001" */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return service name */
    public String getServiceName() {
        return serviceName;
    }

    /** @return kilos */
    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /** @return soap */
    public String getSoapName() {
        return soapName;
    }

    /** @return schedule */
    public Schedule getSchedule() {
        return schedule;
    }

    /** @return payment */
    public Payment getPayment() {
        return payment;
    }

    /** @return price */
    public PriceBreakdownDto getPrice() {
        return price;
    }

    /** {@code schedule} part. */
    public static final class Schedule {
        private String text;

        /** @return "Today, 2–4 PM" */
        public String getText() {
            return text;
        }
    }

    /** {@code payment} part. */
    public static final class Payment {
        private String displayName;

        /** @return "Cash" / "GCash" */
        public String getDisplayName() {
            return displayName;
        }
    }
}
