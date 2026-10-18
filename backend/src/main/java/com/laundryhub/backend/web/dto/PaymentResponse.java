package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.domain.payment.PaymentChoice;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.payment.PaymentStatus;

/**
 * Payment line on 05/06 and A2 "Total · GCash (paid)".
 *
 * @param method       CASH or GCASH
 * @param displayName  "Cash" or "GCash"
 * @param instructions e.g. "Pay the rider upon delivery"
 * @param status       UNPAID, PAID or REFUNDED
 * @param amount       order total
 * @param amountDue    what is still owed
 * @param gcashNumber  customer GCash number, or {@code null}
 * @param referenceNo  receipt / GCash reference, or {@code null}
 * @param paidAt       when it was paid, or {@code null}
 */
public record PaymentResponse(PaymentChoice method, String displayName, String instructions, PaymentStatus status,
                              BigDecimal amount, BigDecimal amountDue, String gcashNumber, String referenceNo,
                              LocalDateTime paidAt) {

    /**
     * @param payment a payment, may be {@code null}
     * @return the DTO, or {@code null}
     */
    // OOP: Polymorphism — display name and instructions come from the PaymentMethod subclass.
    public static PaymentResponse from(PaymentMethod payment) {
        if (payment == null) {
            return null;
        }
        String gcash = payment instanceof GCashPayment g ? g.getGcashNumber() : null;
        return new PaymentResponse(payment.getMethod(), payment.getDisplayName(), payment.getInstructions(),
                payment.getStatus(), payment.getAmount(), payment.getAmountDue(), gcash, payment.getReferenceNo(),
                payment.getPaidAt());
    }
}
