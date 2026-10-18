package com.laundryhub.backend.domain.order;

import java.time.LocalDateTime;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One status change of an order: feeds the "Order status" timeline (07). Created by {@link Order}. */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    private static final int MAX_NOTE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "note", length = MAX_NOTE_LENGTH)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /** For JPA only. */
    protected OrderStatusHistory() {
    }

    OrderStatusHistory(Order order, OrderStatus status, User changedBy, String note) {
        this.order = Check.notNull(order, "Order");
        this.status = Check.notNull(status, "Status");
        this.changedBy = changedBy;
        this.note = Check.optionalText(note, "Note", MAX_NOTE_LENGTH);
        this.changedAt = LocalDateTime.now();
    }

    /** @return database id */
    public Long getId() {
        return id;
    }

    /** @return the status the order moved to */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return who made the change, or {@code null} for the system */
    public User getChangedBy() {
        return changedBy;
    }

    /** @return optional note */
    public String getNote() {
        return note;
    }

    /** @return when it happened */
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
