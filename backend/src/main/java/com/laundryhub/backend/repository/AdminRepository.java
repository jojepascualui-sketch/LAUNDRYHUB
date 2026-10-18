package com.laundryhub.backend.repository;

import java.util.List;

import com.laundryhub.backend.domain.user.Admin;

import org.springframework.data.jpa.repository.JpaRepository;

/** Admin accounts (A4 Staff tab). */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /** @return accounts sorted by name */
    List<Admin> findAllByOrderByFullNameAsc();
}
