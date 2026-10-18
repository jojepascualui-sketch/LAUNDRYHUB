package com.laundryhub.backend.repository;

import java.util.Optional;

import com.laundryhub.backend.domain.payment.PaymentMethod;

import org.springframework.data.jpa.repository.JpaRepository;

/** Payments of orders. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface PaymentRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * @param orderId order id
     * @return the payment of that order
     */
    Optional<PaymentMethod> findByOrderId(Long orderId);
}
