package com.laundryhub.backend.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.order.Order;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The payment of one order. The {@code method} column decides the subclass:
 * {@link CashPayment} or {@link GCashPayment}.
 */
// OOP: Abstraction — shared payment state lives here; each method supplies its own rules.
// OOP: Inheritance — CashPayment and GCashPayment extend this class.
@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "method", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class PaymentMethod extends AuditedEntity implements Payable {

    /** Longest receipt or transaction number accepted. */
    public static final int MAX_REFERENCE_LENGTH = 40;

    // OOP: Encapsulation — status and amount change only through the methods below.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "amount", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Column(name = "reference_no", length = MAX_REFERENCE_LENGTH)
    private String referenceNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** For JPA only. */
    protected PaymentMethod() {
    }

    /**
     * Creates an unpaid payment for the order total and links it to the order.
     *
     * @param order the order being paid
     */
    protected PaymentMethod(Order order) {
        this.order = Check.notNull(order, "Order");
        this.amount = Money.of(order.getTotal());
        order.attachPayment(this);
    }

    /**
     * Marks the payment as received.
     *
     * @param referenceNo receipt or transaction number, may be {@code null}
     * @throws IllegalStateException if it is not unpaid
     */
    protected void markPaid(String referenceNo) {
        if (status != PaymentStatus.UNPAID) {
            throw new IllegalStateException("Payment for " + order.getOrderNo() + " is already " + status);
        }
        this.referenceNo = Check.optionalText(referenceNo, "Reference number", MAX_REFERENCE_LENGTH);
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Records that the money was returned to the customer.
     *
     * @throws IllegalStateException if the payment was not paid
     */
    public void markRefunded() {
        if (status != PaymentStatus.PAID) {
            throw new IllegalStateException("Only a paid payment can be refunded");
        }
        status = PaymentStatus.REFUNDED;
    }

    /**
     * Follows a changed order total (after weighing).
     *
     * @param newAmount new order total
     * @throws IllegalStateException if the payment is no longer unpaid
     */
    public void changeAmount(BigDecimal newAmount) {
        if (status != PaymentStatus.UNPAID) {
            throw new IllegalStateException("Amount cannot change after payment");
        }
        this.amount = Money.of(Check.nonNegative(newAmount, "Amount"));
    }

    /** @return which method this is (Cash or GCash) */
    // OOP: Polymorphism — CashPayment and GCashPayment answer with their own constant.
    public abstract PaymentChoice getMethod();

    @Override
    public BigDecimal getAmountDue() {
        return status == PaymentStatus.UNPAID ? amount : Money.ZERO;
    }

    @Override
    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    /** @return the order being paid */
    public Order getOrder() {
        return order;
    }

    /** @return the order total at the time of payment */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return payment status */
    public PaymentStatus getStatus() {
        return status;
    }

    /** @return receipt or transaction number, or {@code null} */
    public String getReferenceNo() {
        return referenceNo;
    }

    /** @return when it was paid, or {@code null} */
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
