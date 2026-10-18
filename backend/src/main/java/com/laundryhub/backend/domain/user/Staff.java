package com.laundryhub.backend.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Counter staff: handles orders and statuses, but not prices or accounts. */
// OOP: Inheritance — Staff is a ConsoleUser, which is a User.
@Entity
@DiscriminatorValue("STAFF")
public class Staff extends ConsoleUser {

    /** For JPA only. */
    protected Staff() {
    }

    /**
     * Creates a staff account.
     *
     * @param fullName full name
     * @param email    sign-in e-mail
     * @param position label, e.g. "Counter staff"
     */
    public Staff(String fullName, String email, String position) {
        super(fullName, email, position);
    }

    @Override
    public Role getRole() {
        return Role.STAFF;
    }

    @Override
    public boolean canManagePricing() {
        return false;
    }

    @Override
    public boolean canManageAccounts() {
        return false;
    }
}
