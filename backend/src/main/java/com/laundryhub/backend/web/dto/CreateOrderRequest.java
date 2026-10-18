package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.payment.PaymentChoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 05 "Place order". Prices are not sent: the server calculates them.
 *
 * @param serviceType    02 choice
 * @param weightKg       03 kilos, at least the minimum load
 * @param soapId         03 soap
 * @param scheduleDate   day of the chosen time chip
 * @param slotLabel      chip text, e.g. "2–4 PM" or "morning" (see {@code GET /api/orders/slots})
 * @param pin            04a pin; required for Pickup &amp; Delivery, ignored for Walk-in
 * @param addressDetails 04a "Address details" (house no., street, barangay, landmark)
 * @param paymentMethod  CASH or GCASH
 * @param gcashNumber    "GCash mobile number", required for GCASH
 */
public record CreateOrderRequest(
        @NotNull ServiceCode serviceType,
        @NotNull @Positive BigDecimal weightKg,
        @NotNull Long soapId,
        @NotNull LocalDate scheduleDate,
        @NotBlank String slotLabel,
        @Valid PinRequest pin,
        @Size(max = 255) String addressDetails,
        @NotNull PaymentChoice paymentMethod,
        String gcashNumber) {
}
