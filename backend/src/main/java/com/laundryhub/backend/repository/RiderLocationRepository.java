package com.laundryhub.backend.repository;

import java.util.Optional;

import com.laundryhub.backend.domain.rider.RiderLocation;
import com.laundryhub.backend.domain.user.Rider;

import org.springframework.data.jpa.repository.JpaRepository;

/** Rider GPS points. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface RiderLocationRepository extends JpaRepository<RiderLocation, Long> {

    /**
     * @param rider a rider
     * @return the rider's latest GPS point
     */
    Optional<RiderLocation> findFirstByRiderOrderByRecordedAtDesc(Rider rider);
}
