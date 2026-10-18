package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the customer order flow (01–05) needs to show prices and choices.
 * Totals shown from this are estimates; {@code POST /api/orders/quote} is authoritative.
 *
 * @param ratePerKg        "₱30 per kg"
 * @param minKg            "Minimum load 8 kg"; the − button is disabled here
 * @param deliveryFee      "+ ₱60 delivery fee"
 * @param deliveryRadiusKm pins farther than this are rejected
 * @param cashEnabled      whether Cash is offered on 05
 * @param gcashEnabled     whether GCash is offered on 05
 * @param services         the two service cards on 02
 * @param soaps            available soaps for the 2×2 grid on 03
 */
public record PricingResponse(BigDecimal ratePerKg, BigDecimal minKg, BigDecimal deliveryFee,
                              BigDecimal deliveryRadiusKm, boolean cashEnabled, boolean gcashEnabled,
                              List<ServiceOptionResponse> services, List<SoapResponse> soaps) {
}
