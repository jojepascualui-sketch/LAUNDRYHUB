package com.laundryhub.backend.repository;

import java.util.List;

import com.laundryhub.backend.domain.user.Staff;

import org.springframework.data.jpa.repository.JpaRepository;

/** Staff accounts (A4 Staff tab). */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface StaffRepository extends JpaRepository<Staff, Long> {

    /** @return accounts sorted by name */
    List<Staff> findAllByOrderByFullNameAsc();
}
