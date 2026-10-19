package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** {@code POST /api/orders/quote} response and the {@code price} of an order. Filled by Gson. */
public final class PriceBreakdownDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private BigDecimal billableKg;
    private BigDecimal ratePerKg;
    private BigDecimal laundryCost;
    private String soapName;
    private BigDecimal soapAddOnPerKg;
    private BigDecimal soapCost;
    private BigDecimal deliveryFee;
    private BigDecimal total;

    /** For Gson. */
    public PriceBreakdownDto() {
    }

    /** @return kilos charged */
    public BigDecimal getBillableKg() {
        return billableKg;
    }

    /** @return price per kilo */
    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }

    /** @return kilos × rate */
    public BigDecimal getLaundryCost() {
        return laundryCost;
    }

    /** @return soap */
    public String getSoapName() {
        return soapName;
    }

    /** @return soap price per kilo */
    public BigDecimal getSoapAddOnPerKg() {
        return soapAddOnPerKg;
    }

    /** @return kilos × add-on */
    public BigDecimal getSoapCost() {
        return soapCost;
    }

    /** @return delivery fee */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** @return total */
    public BigDecimal getTotal() {
        return total;
    }
}
