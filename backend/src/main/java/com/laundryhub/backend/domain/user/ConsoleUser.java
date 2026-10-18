package com.laundryhub.backend.domain.user;

import com.laundryhub.backend.domain.common.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * An account that uses the JavaFX admin console: {@link Admin} or {@link Staff}.
 * Carries the position label shown in the A4 Staff tab ("Owner", "Counter staff").
 */
// OOP: Abstraction — console screens depend on "a console user" and its permissions.
// OOP: Inheritance — Admin and Staff share the position label and console access.
@Entity
public abstract class ConsoleUser extends User {

    /** Longest position label accepted. */
    public static final int MAX_POSITION_LENGTH = 60;

    @Column(name = "position", length = MAX_POSITION_LENGTH)
    private String position;

    /** For JPA only. */
    protected ConsoleUser() {
    }

    /**
     * Creates a console account.
     *
     * @param fullName full name
     * @param email    e-mail used to sign in ("Staff email")
     * @param position label shown in A4, e.g. "Owner"
     */
    protected ConsoleUser(String fullName, String email, String position) {
        super(fullName, email, null);
        changePosition(position);
    }

    @Override
    public boolean canUseAdminConsole() {
        return true;
    }

    /** @return whether this account may edit prices, soaps and payment methods (A3) */
    // OOP: Polymorphism — Admin and Staff answer differently.
    public abstract boolean canManagePricing();

    /** @return whether this account may add and edit customers, riders and staff (A4) */
    // OOP: Polymorphism — Admin and Staff answer differently.
    public abstract boolean canManageAccounts();

    /**
     * Changes the position label.
     *
     * @param position new label, e.g. "Counter staff"
     */
    public final void changePosition(String position) {
        this.position = Check.text(position, "Position", MAX_POSITION_LENGTH);
    }

    /** @return position label */
    public String getPosition() {
        return position;
    }
}
