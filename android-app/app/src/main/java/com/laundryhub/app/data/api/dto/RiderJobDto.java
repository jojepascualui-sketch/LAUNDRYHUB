package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** A job from {@code /api/rider/jobs}. Filled by Gson. */
public final class RiderJobDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long id;
    private String orderNo;
    private String type;
    private String status;
    private BigDecimal distanceKm;
    private String customerName;
    private String customerInitials;
    private String customerMobile;
    private Pin customerPin;
    private Point shop;
    private String scheduleText;
    private BigDecimal billableKg;
    private BigDecimal total;
    private Payment payment;
    private List<Step> steps;
    private int currentStepIndex;
    private boolean awaitingCash;

    /** For Gson. */
    public RiderJobDto() {
    }

    /** @return job id */
    public Long getId() {
        return id;
    }

    /** @return order number */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return PICKUP or RETURN_DELIVERY */
    public String getType() {
        return type;
    }

    /** @return job status */
    public String getStatus() {
        return status;
    }

    /** @return distance in km, or {@code null} */
    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    /** @return customer */
    public String getCustomerName() {
        return customerName;
    }

    /** @return initials */
    public String getCustomerInitials() {
        return customerInitials;
    }

    /** @return phone, or {@code null} */
    public String getCustomerMobile() {
        return customerMobile;
    }

    /** @return customer pin and address */
    public Pin getCustomerPin() {
        return customerPin;
    }

    /** @return shop point, or {@code null} */
    public Point getShop() {
        return shop;
    }

    /** @return "Today, 4–6 PM" */
    public String getScheduleText() {
        return scheduleText;
    }

    /** @return kilos */
    public BigDecimal getBillableKg() {
        return billableKg;
    }

    /** @return order total */
    public BigDecimal getTotal() {
        return total;
    }

    /** @return payment strip */
    public Payment getPayment() {
        return payment;
    }

    /** @return checklist */
    public List<Step> getSteps() {
        return steps == null ? Collections.emptyList() : steps;
    }

    /** @return index of the next step */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /** @return whether the next step is the cash step */
    public boolean isAwaitingCash() {
        return awaitingCash;
    }

    /** A map point. */
    public static class Point {
        private BigDecimal latitude;
        private BigDecimal longitude;

        /** @return latitude */
        public BigDecimal getLatitude() {
            return latitude;
        }

        /** @return longitude */
        public BigDecimal getLongitude() {
            return longitude;
        }
    }

    /** The customer pin with its addresses. */
    public static final class Pin extends Point {
        private String addressDetails;
        private String geocodedAddress;

        /** @return what the customer typed */
        public String getAddressDetails() {
            return addressDetails;
        }

        /** @return what Google found */
        public String getGeocodedAddress() {
            return geocodedAddress;
        }
    }

    /** Payment strip. */
    public static final class Payment {
        private String method;

        /** @return CASH or GCASH */
        public String getMethod() {
            return method;
        }
    }

    /** One checklist row. */
    public static final class Step {
        private String title;
        private String actionLabel;

        /** @return step title */
        public String getTitle() {
            return title;
        }

        /** @return button text */
        public String getActionLabel() {
            return actionLabel;
        }
    }
}
