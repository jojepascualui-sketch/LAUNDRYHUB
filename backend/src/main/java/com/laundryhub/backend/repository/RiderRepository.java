package com.laundryhub.backend.repository;

import java.util.List;

import com.laundryhub.backend.domain.user.Rider;

import org.springframework.data.jpa.repository.JpaRepository;

/** Rider accounts (A4 Riders tab, A2 Assign rider). */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface RiderRepository extends JpaRepository<Rider, Long> {

    /** @return riders currently on duty, sorted by name */
    List<Rider> findByOnDutyTrueOrderByFullNameAsc();

    /** @return all riders sorted by name (A4 Riders tab, A2 Assign rider) */
    List<Rider> findAllByOrderByFullNameAsc();
}
