package com.laundryhub.backend.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A customer who books laundry (screens 00b–07). */
// OOP: Inheritance — a Customer is a User.
@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends User {

    /** For JPA only. */
    protected Customer() {
    }

    /**
     * Creates a customer account (sign-up screen 00b).
     *
     * @param fullName     full name
     * @param email        e-mail address
     * @param mobileNumber mobile number 09XXXXXXXXX
     */
    public Customer(String fullName, String email, String mobileNumber) {
        super(fullName, email, mobileNumber);
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }
}
