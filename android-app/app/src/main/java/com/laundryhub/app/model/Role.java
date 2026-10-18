package com.laundryhub.app.model;

import androidx.annotation.NonNull;

/**
 * Account roles from the backend. Each role knows where it goes after sign-in
 * (CLAUDE.md §3 navigation): customers to Home, riders to their bookings, and admin/staff to
 * the desktop console, which this app does not contain.
 */
public enum Role {
    /** Books laundry in this app. */
    CUSTOMER {
        @NonNull
        @Override
        public AppHome home() {
            return AppHome.CUSTOMER_HOME;
        }
    },
    /** Accepts pickup and delivery jobs in this app. */
    RIDER {
        @NonNull
        @Override
        public AppHome home() {
            return AppHome.RIDER_HOME;
        }
    },
    /** Shop owner: uses the JavaFX admin console. */
    ADMIN {
        @NonNull
        @Override
        public AppHome home() {
            return AppHome.ADMIN_CONSOLE;
        }
    },
    /** Counter staff: uses the JavaFX admin console. */
    STAFF {
        @NonNull
        @Override
        public AppHome home() {
            return AppHome.ADMIN_CONSOLE;
        }
    };

    /** @return the first screen after sign-in for this role */
    // OOP: Polymorphism — each role constant answers with its own destination.
    @NonNull
    public abstract AppHome home();

    /** @return whether this role uses the phone app (and so keeps a session on the phone) */
    public boolean usesMobileApp() {
        return home() != AppHome.ADMIN_CONSOLE;
    }

    /**
     * @param apiRole role text from the API, e.g. "RIDER"
     * @return the role
     * @throws IllegalArgumentException if the text is not a known role
     */
    @NonNull
    public static Role fromApi(@NonNull String apiRole) {
        return valueOf(apiRole);
    }

    /** Where a signed-in account lands. */
    public enum AppHome {
        /** 01 Home. */
        CUSTOMER_HOME,
        /** R1 Available bookings. */
        RIDER_HOME,
        /** "Use the admin console" message. */
        ADMIN_CONSOLE
    }
}
