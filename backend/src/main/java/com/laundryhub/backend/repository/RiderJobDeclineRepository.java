package com.laundryhub.backend.repository;

import com.laundryhub.backend.domain.rider.RiderJobDecline;

import org.springframework.data.jpa.repository.JpaRepository;

/** Declined jobs per rider. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface RiderJobDeclineRepository extends JpaRepository<RiderJobDecline, RiderJobDecline.Key> {
}
