package com.laundryhub.app.ui.order.location;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.laundryhub.app.model.Location;

/** "Use my current location" with {@link FusedLocationProviderClient#getCurrentLocation}. */
// OOP: Inheritance — implements the CurrentLocationProvider contract.
public class FusedCurrentLocationProvider implements CurrentLocationProvider {

    // OOP: Encapsulation — the client and the pending request stay private.
    private final FusedLocationProviderClient client;
    @Nullable
    private CancellationTokenSource pending;

    /**
     * @param context any context
     */
    public FusedCurrentLocationProvider(@NonNull Context context) {
        client = LocationServices.getFusedLocationProviderClient(context);
    }

    // OOP: Polymorphism — the Google Play services implementation of request().
    @SuppressLint("MissingPermission") // The caller requests the permission first (see ScheduleStepFragment).
    @Override
    public void request(boolean precise, @NonNull Callback callback) {
        cancel();
        CancellationTokenSource token = new CancellationTokenSource();
        pending = token;
        int priority = precise ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        client.getCurrentLocation(priority, token.getToken())
                .addOnSuccessListener(fix -> {
                    if (token != pending) {
                        return;
                    }
                    pending = null;
                    if (fix == null) {
                        callback.onUnavailable();
                    } else {
                        callback.onLocation(Location.of(fix.getLatitude(), fix.getLongitude()));
                    }
                })
                .addOnFailureListener(e -> {
                    if (token == pending) {
                        pending = null;
                        callback.onUnavailable();
                    }
                });
    }

    @Override
    public void cancel() {
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
    }
}
