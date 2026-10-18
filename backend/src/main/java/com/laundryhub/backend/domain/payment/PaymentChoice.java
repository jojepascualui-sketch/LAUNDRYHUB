package com.laundryhub.backend.domain.payment;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.order.Order;

/** The payment method the customer picks on screen 05. Stored in {@code payments.method}. */
public enum PaymentChoice {

    /** "Pay the rider upon delivery" / "Pay at the counter". */
    CASH {
        @Override
        public PaymentMethod attachTo(Order order, String gcashNumber) {
            return new CashPayment(order);
        }

        @Override
        public boolean isEnabledIn(PricingSettings settings) {
            return settings.isCashEnabled();
        }

        @Override
        public String getDisplayName() {
            return CashPayment.DISPLAY_NAME;
        }
    },

    /** "Pay instantly from your e-wallet"; needs the customer's GCash mobile number. */
    GCASH {
        @Override
        public PaymentMethod attachTo(Order order, String gcashNumber) {
            return new GCashPayment(order, gcashNumber);
        }

        @Override
        public boolean isEnabledIn(PricingSettings settings) {
            return settings.isGcashEnabled();
        }

        @Override
        public String getDisplayName() {
            return GCashPayment.DISPLAY_NAME;
        }
    };

    /**
     * Creates the matching payment and links it to the order.
     *
     * @param order       the new order
     * @param gcashNumber customer GCash number (GCash only; ignored for Cash)
     * @return the payment
     */
    // OOP: Polymorphism — each constant builds its own PaymentMethod subclass.
    public abstract PaymentMethod attachTo(Order order, String gcashNumber);

    /**
     * @param settings current pricing settings (A3 "Payment methods")
     * @return whether the admin has this method switched on
     */
    public abstract boolean isEnabledIn(PricingSettings settings);

    /** @return name shown on screen 05, e.g. "GCash" */
    public abstract String getDisplayName();
}
