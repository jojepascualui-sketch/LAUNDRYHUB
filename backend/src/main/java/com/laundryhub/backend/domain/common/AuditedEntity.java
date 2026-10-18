package com.laundryhub.backend.domain.common;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Base class for tables with an auto-increment id and created/updated timestamps.
 */
// OOP: Inheritance — every audited entity reuses the id and timestamp handling.
@MappedSuperclass
public abstract class AuditedEntity {

    // OOP: Encapsulation — the id and timestamps are only set by JPA and the lifecycle hooks.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** @return database id, or {@code null} before the entity is saved */
    public Long getId() {
        return id;
    }

    /** @return when the row was created */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return when the row was last changed */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
