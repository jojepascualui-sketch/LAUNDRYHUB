package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Marks an order paid from the console: cash at the counter, or a GCash transfer.
 *
 * @param referenceNo receipt or GCash reference (required for GCash)
 */
public record ConfirmPaymentRequest(@Size(max = 40) String referenceNo) {
}
