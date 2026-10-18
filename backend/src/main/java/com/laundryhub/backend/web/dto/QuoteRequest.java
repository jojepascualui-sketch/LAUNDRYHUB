package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.order.ServiceCode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Live estimate for 02–05: service, kilos and soap.
 *
 * @param serviceType Walk-in or Pickup &amp; Delivery
 * @param weightKg    kilos; below the minimum load is rejected
 * @param soapId      chosen soap
 */
public record QuoteRequest(@NotNull ServiceCode serviceType, @NotNull @Positive BigDecimal weightKg,
                           @NotNull Long soapId) {
}
