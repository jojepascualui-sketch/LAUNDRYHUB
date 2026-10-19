package com.laundryhub.app.ui.order.location;

import androidx.annotation.NonNull;

import com.laundryhub.app.model.Location;

/** One GPS fix for "Use my current location" (04a). The caller has already checked permission. */
// OOP: Abstraction — the step screen does not know Google Play services provides the fix.
public interface CurrentLocationProvider {

    /** Result of a location request, delivered on the main thread. */
    interface Callback {
        /**
         * @param location where the phone is
         */
        void onLocation(@NonNull Location location);

        /** No fix could be obtained (location off, no signal, error). */
        void onUnavailable();
    }

    /**
     * @param precise  whether precise (fine) location was granted
     * @param callback the result
     */
    void request(boolean precise, @NonNull Callback callback);

    /** Cancels a pending request (screen closed). */
    void cancel();
}
