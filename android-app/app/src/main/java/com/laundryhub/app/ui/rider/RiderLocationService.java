package com.laundryhub.app.ui.rider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.rider.RiderRepository;
import com.laundryhub.app.model.Location;

import java.math.BigDecimal;

/**
 * While the rider is on duty: a foreground service that sends the GPS position to
 * {@code POST /api/rider/location} every 10 seconds (customer tracking, nearest bookings).
 * Started when the rider goes on duty, stopped when off duty or signed out.
 */
// OOP: Inheritance — an Android Service.
public class RiderLocationService extends Service {

    /** How often a position is sent. */
    public static final long INTERVAL_MS = 10_000L;

    private static final String CHANNEL_ID = "rider_on_duty";
    private static final int NOTIFICATION_ID = 1001;

    // OOP: Encapsulation — the location client and callback live only inside the service.
    private FusedLocationProviderClient client;
    private RiderRepository repository;
    private boolean updating;

    private final LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            android.location.Location fix = result.getLastLocation();
            if (fix != null) {
                send(fix);
            }
        }
    };

    /**
     * Starts sharing the position (does nothing without location permission).
     *
     * @param context any context
     */
    public static void start(@NonNull Context context) {
        if (hasLocationPermission(context)) {
            ContextCompat.startForegroundService(context, new Intent(context, RiderLocationService.class));
        }
    }

    /**
     * Stops sharing the position.
     *
     * @param context any context
     */
    public static void stop(@NonNull Context context) {
        context.stopService(new Intent(context, RiderLocationService.class));
    }

    /**
     * @param context any context
     * @return whether fine or coarse location was granted
     */
    public static boolean hasLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        client = LocationServices.getFusedLocationProviderClient(this);
        repository = LaundryHubApp.container(this).riderRepository();
    }

    // OOP: Polymorphism — Android calls these Service overrides.
    @SuppressLint("MissingPermission") // Checked just above the request.
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (!hasLocationPermission(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        createChannel();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type);
        if (!updating) {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
                    .setMinUpdateIntervalMillis(INTERVAL_MS)
                    .build();
            client.requestLocationUpdates(request, callback, Looper.getMainLooper());
            updating = true;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (client != null) {
            client.removeLocationUpdates(callback);
        }
        updating = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void send(android.location.Location fix) {
        BigDecimal accuracy = fix.hasAccuracy() ? BigDecimal.valueOf(fix.getAccuracy()) : null;
        repository.sendLocation(Location.of(fix.getLatitude(), fix.getLongitude()), accuracy, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Boolean done) {
            }

            @Override
            public void onError(@NonNull ApiException error) {
                // Signed out or no longer a rider: stop. Network errors: try again with the next fix.
                if (error.getStatus() == RiderHomeViewModel.UNAUTHORIZED) {
                    stopSelf();
                }
            }
        });
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.location_channel),
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                RiderHomeActivity.intent(this, RiderTab.AVAILABLE), PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_truck_white)
                .setContentTitle(getString(R.string.location_notification_title))
                .setContentText(getString(R.string.location_notification_text))
                .setContentIntent(open)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }
}
