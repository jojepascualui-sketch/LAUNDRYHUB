package com.laundryhub.backend.repository;

import java.util.List;

import com.laundryhub.backend.domain.catalog.Soap;

import org.springframework.data.jpa.repository.JpaRepository;

/** Fabric soaps. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface SoapRepository extends JpaRepository<Soap, Long> {

    /** @return all soaps in display order (A3) */
    List<Soap> findAllByOrderBySortOrderAsc();

    /** @return soaps customers can choose, in display order (screen 03) */
    List<Soap> findByAvailableTrueOrderBySortOrderAsc();

    /**
     * @param name soap name
     * @return whether a soap already has that name
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * @param name soap name
     * @param id   soap being edited
     * @return whether another soap already has that name
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** @return the soap shown last, to place a new soap after it */
    java.util.Optional<Soap> findFirstByOrderBySortOrderDesc();
}
