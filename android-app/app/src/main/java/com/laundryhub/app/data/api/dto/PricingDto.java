package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** {@code GET /api/pricing}. Filled by Gson. */
public final class PricingDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private BigDecimal ratePerKg;
    private BigDecimal minKg;
    private BigDecimal deliveryFee;
    private boolean cashEnabled;
    private boolean gcashEnabled;
    private List<SoapDto> soaps;

    /** For Gson. */
    public PricingDto() {
    }

    /** @return price per kilo */
    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }

    /** @return minimum load */
    public BigDecimal getMinKg() {
        return minKg;
    }

    /** @return delivery fee */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** @return whether Cash is offered */
    public boolean isCashEnabled() {
        return cashEnabled;
    }

    /** @return whether GCash is offered */
    public boolean isGcashEnabled() {
        return gcashEnabled;
    }

    /** @return available soaps */
    public List<SoapDto> getSoaps() {
        return soaps == null ? Collections.emptyList() : soaps;
    }

    /** One soap. */
    public static final class SoapDto {
        private Long id;
        private String name;
        private String description;
        private BigDecimal addOnPerKg;
        private String tintHex;

        /** For Gson. */
        public SoapDto() {
        }

        /** @return id */
        public Long getId() {
            return id;
        }

        /** @return name */
        public String getName() {
            return name;
        }

        /** @return card text */
        public String getDescription() {
            return description;
        }

        /** @return pesos per kilo */
        public BigDecimal getAddOnPerKg() {
            return addOnPerKg;
        }

        /** @return icon tile color */
        public String getTintHex() {
            return tintHex;
        }
    }
}
