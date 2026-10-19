package com.laundryhub.app.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * The order being built in the wizard (02–05). Immutable: every change returns a new draft.
 * Mirrors the prototype state: service, kg, soap, slot, pin, payment.
 */
public final class OrderDraft {

    /** A chosen time chip. */
    public record Slot(@NonNull String date, @NonNull String label, @NonNull String text) {
        /** Validates the slot. */
        public Slot {
            if (date == null || label == null || text == null) {
                throw new IllegalArgumentException("Slot date, label and text are required");
            }
        }
    }

    // OOP: Encapsulation — private final fields, changed only through with...() copies.
    @Nullable
    private final ServiceType service;
    private final int kg;
    @Nullable
    private final Long soapId;
    @Nullable
    private final Slot slot;
    @Nullable
    private final Location pin;
    /** {@code null} = not checked yet or unknown; {@code false} = outside the delivery radius. */
    @Nullable
    private final Boolean pinInside;
    @NonNull
    private final String addressDetails;
    @NonNull
    private final PaymentOption payment;
    @NonNull
    private final String gcashNumber;

    private OrderDraft(@Nullable ServiceType service, int kg, @Nullable Long soapId, @Nullable Slot slot,
                       @Nullable Location pin, @Nullable Boolean pinInside, @Nullable String addressDetails,
                       @NonNull PaymentOption payment, @Nullable String gcashNumber) {
        if (kg <= 0) {
            throw new IllegalArgumentException("Weight must be more than 0 kg");
        }
        this.service = service;
        this.kg = kg;
        this.soapId = soapId;
        this.slot = slot;
        this.pin = pin;
        this.pinInside = pin == null ? null : pinInside;
        this.addressDetails = addressDetails == null ? "" : addressDetails;
        this.payment = Objects.requireNonNull(payment, "Payment method is required");
        this.gcashNumber = gcashNumber == null ? "" : gcashNumber;
    }

    /**
     * The prototype's starting state: no service, minimum kilos, first soap, no slot, Cash.
     *
     * @param minKg  minimum load in whole kilos
     * @param soapId default soap, or {@code null} until prices load
     * @return a fresh draft
     */
    @NonNull
    public static OrderDraft start(int minKg, @Nullable Long soapId) {
        return new OrderDraft(null, minKg, soapId, null, null, null, null, PaymentOption.CASH, null);
    }

    /**
     * Picks a service; a different service clears the time slot (its chips differ).
     *
     * @param newService the service
     * @return the new draft
     */
    @NonNull
    public OrderDraft withService(@NonNull ServiceType newService) {
        Slot keptSlot = newService == service ? slot : null;
        return new OrderDraft(newService, kg, soapId, keptSlot, pin, pinInside, addressDetails, payment, gcashNumber);
    }

    /**
     * @param newKg kilos, more than 0
     * @return the new draft
     */
    @NonNull
    public OrderDraft withKg(int newKg) {
        return new OrderDraft(service, newKg, soapId, slot, pin, pinInside, addressDetails, payment, gcashNumber);
    }

    /**
     * @param newSoapId the soap
     * @return the new draft
     */
    @NonNull
    public OrderDraft withSoap(long newSoapId) {
        return new OrderDraft(service, kg, newSoapId, slot, pin, pinInside, addressDetails, payment, gcashNumber);
    }

    /**
     * @param newSlot the time chip
     * @return the new draft
     */
    @NonNull
    public OrderDraft withSlot(@NonNull Slot newSlot) {
        return new OrderDraft(service, kg, soapId, newSlot, pin, pinInside, addressDetails, payment, gcashNumber);
    }

    /**
     * Moves the pin; whether it is inside the delivery radius is unknown until it is checked.
     *
     * @param newPin the pinned point
     * @return the new draft
     */
    @NonNull
    public OrderDraft withPin(@NonNull Location newPin) {
        return new OrderDraft(service, kg, soapId, slot, newPin, null, addressDetails, payment, gcashNumber);
    }

    /**
     * Records the server's delivery-radius check for the current pin.
     *
     * @param inside whether the pin is inside the delivery radius
     * @return the new draft
     * @throws IllegalStateException if there is no pin
     */
    @NonNull
    public OrderDraft withPinCheck(boolean inside) {
        if (pin == null) {
            throw new IllegalStateException("No pin to check");
        }
        return new OrderDraft(service, kg, soapId, slot, pin, inside, addressDetails, payment, gcashNumber);
    }

    /**
     * @param details "Address details"
     * @return the new draft
     */
    @NonNull
    public OrderDraft withAddressDetails(@NonNull String details) {
        return new OrderDraft(service, kg, soapId, slot, pin, pinInside, details, payment, gcashNumber);
    }

    /**
     * @param newPayment Cash or GCash
     * @return the new draft
     */
    @NonNull
    public OrderDraft withPayment(@NonNull PaymentOption newPayment) {
        return new OrderDraft(service, kg, soapId, slot, pin, pinInside, addressDetails, newPayment, gcashNumber);
    }

    /**
     * @param number "GCash mobile number" as typed
     * @return the new draft
     */
    @NonNull
    public OrderDraft withGcashNumber(@NonNull String number) {
        return new OrderDraft(service, kg, soapId, slot, pin, pinInside, addressDetails, payment, number);
    }

    /** @return the service, or {@code null} before step 1 */
    @Nullable
    public ServiceType getService() {
        return service;
    }

    /** @return kilos on the stepper */
    public int getKg() {
        return kg;
    }

    /** @return the soap id, or {@code null} */
    @Nullable
    public Long getSoapId() {
        return soapId;
    }

    /** @return the time chip, or {@code null} */
    @Nullable
    public Slot getSlot() {
        return slot;
    }

    /** @return the pinned location, or {@code null} */
    @Nullable
    public Location getPin() {
        return pin;
    }

    /** @return whether the pin is inside the delivery radius; {@code null} if unknown or no pin */
    @Nullable
    public Boolean getPinInside() {
        return pinInside;
    }

    /** @return address details, never {@code null} */
    @NonNull
    public String getAddressDetails() {
        return addressDetails;
    }

    /** @return the payment method */
    @NonNull
    public PaymentOption getPayment() {
        return payment;
    }

    /** @return the GCash number as typed, never {@code null} */
    @NonNull
    public String getGcashNumber() {
        return gcashNumber;
    }
}
