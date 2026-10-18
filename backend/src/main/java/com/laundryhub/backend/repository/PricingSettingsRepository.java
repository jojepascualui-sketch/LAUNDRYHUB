package com.laundryhub.backend.repository;

import java.util.Optional;

import com.laundryhub.backend.domain.catalog.PricingSettings;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The single pricing row. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface PricingSettingsRepository extends JpaRepository<PricingSettings, Long> {

    /** @return the pricing settings row */
    default PricingSettings getSettings() {
        return findById(PricingSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("pricing_settings row is missing"));
    }

    /**
     * Reads the row with a write lock. Placing an order holds it, so order numbers are handed out
     * one at a time and an admin price change cannot happen halfway through an order.
     *
     * @param id {@link PricingSettings#SINGLETON_ID}
     * @return the locked row
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PricingSettings p where p.id = :id")
    Optional<PricingSettings> findByIdForUpdate(@Param("id") Long id);

    /** @return the pricing settings row, locked until the transaction ends */
    default PricingSettings lockSettings() {
        return findByIdForUpdate(PricingSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("pricing_settings row is missing"));
    }
}
