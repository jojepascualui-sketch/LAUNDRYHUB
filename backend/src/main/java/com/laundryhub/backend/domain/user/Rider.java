package com.laundryhub.backend.domain.user;

import com.laundryhub.backend.domain.common.Check;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

/**
 * A rider who accepts pickup and return-delivery jobs (screens R1–R5).
 * Rider-only fields live in the {@code riders} table.
 */
// OOP: Inheritance — a Rider is a User with a plate number and an on-duty switch.
@Entity
@DiscriminatorValue("RIDER")
@SecondaryTable(name = "riders", pkJoinColumns = @PrimaryKeyJoinColumn(name = "user_id"))
public class Rider extends User {

    /** Longest plate number accepted. */
    public static final int MAX_PLATE_LENGTH = 20;

    // OOP: Encapsulation — duty can only change through goOnDuty()/goOffDuty().
    @Column(table = "riders", name = "plate_number", nullable = false, length = MAX_PLATE_LENGTH)
    private String plateNumber;

    @Column(table = "riders", name = "on_duty", nullable = false)
    private boolean onDuty;

    /** For JPA only. */
    protected Rider() {
    }

    /**
     * Creates an off-duty rider.
     *
     * @param fullName     full name
     * @param email        e-mail address
     * @param mobileNumber mobile number 09XXXXXXXXX
     * @param plateNumber  vehicle plate, shown as "Your rider · [PLATE NO.]"
     */
    public Rider(String fullName, String email, String mobileNumber, String plateNumber) {
        super(fullName, email, mobileNumber);
        changePlateNumber(plateNumber);
    }

    @Override
    public Role getRole() {
        return Role.RIDER;
    }

    /**
     * Changes the vehicle plate number.
     *
     * @param plateNumber new plate number
     */
    public final void changePlateNumber(String plateNumber) {
        this.plateNumber = Check.text(plateNumber, "Plate number", MAX_PLATE_LENGTH).toUpperCase();
    }

    /** Turns the "On duty" switch on so the rider receives bookings (R1). */
    public void goOnDuty() {
        onDuty = true;
    }

    /** Turns the switch off; the bookings list is replaced by the off-duty card (R5). */
    public void goOffDuty() {
        onDuty = false;
    }

    /** @return vehicle plate number */
    public String getPlateNumber() {
        return plateNumber;
    }

    /** @return whether the rider currently receives bookings */
    public boolean isOnDuty() {
        return onDuty;
    }
}
