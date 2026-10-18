package com.laundryhub.backend.repository;

import com.laundryhub.backend.domain.catalog.ShopSettings;

import org.springframework.data.jpa.repository.JpaRepository;

/** The single shop row. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface ShopSettingsRepository extends JpaRepository<ShopSettings, Long> {

    /** @return the shop settings row */
    default ShopSettings getSettings() {
        return findById(ShopSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("shop_settings row is missing"));
    }
}
