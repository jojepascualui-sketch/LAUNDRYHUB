package com.laundryhub.backend.repository;

import java.util.List;

import com.laundryhub.backend.domain.user.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

/** Customer accounts (A4 Customers tab). */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** @return customers sorted by name */
    List<Customer> findAllByOrderByFullNameAsc();
}
