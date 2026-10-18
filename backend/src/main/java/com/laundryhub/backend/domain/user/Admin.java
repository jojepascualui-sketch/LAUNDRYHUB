package com.laundryhub.backend.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** The shop owner: full access to the admin console (A1–A5). */
// OOP: Inheritance — an Admin is a ConsoleUser, which is a User.
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends ConsoleUser {

    /** Position label of the first admin, as on the A1 sidebar ("Admin · Owner"). */
    public static final String DEFAULT_POSITION = "Owner";

    /** For JPA only. */
    protected Admin() {
    }

    /**
     * Creates an admin account.
     *
     * @param fullName full name
     * @param email    sign-in e-mail
     * @param position label, e.g. {@link #DEFAULT_POSITION}
     */
    public Admin(String fullName, String email, String position) {
        super(fullName, email, position);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public boolean canManagePricing() {
        return true;
    }

    @Override
    public boolean canManageAccounts() {
        return true;
    }
}
