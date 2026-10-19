package com.laundryhub.app.ui.rider;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ActivityActiveJobBinding;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.rider.JobStep;
import com.laundryhub.app.model.rider.JobType;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.ui.auth.SignInActivity;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;
import com.laundryhub.app.ui.order.location.CurrentLocationProvider;
import com.laundryhub.app.ui.order.location.FusedCurrentLocationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * R2-rider-pickup-job / R3-rider-delivery-cash / R4-rider-job-done: the accepted job with map,
 * "Navigate in Google Maps", customer card with Call, step checklist and the big step button;
 * after the last step, "Job completed" and "Back to bookings".
 */
// OOP: Inheritance — an AppCompatActivity.
public class ActiveJobActivity extends AppCompatActivity {

    /** Intent extra: the job id. */
    public static final String EXTRA_JOB_ID = "com.laundryhub.app.extra.JOB_ID";

    private static final long NO_JOB = -1L;
    private static final String MAP_TAG = "job_map";
    /** Google Maps turn-by-turn ("google.navigation:q=lat,lng"). */
    private static final String NAVIGATION_URI = "google.navigation:q=";
    private static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    /** Browser fallback when the Google Maps app is missing. */
    private static final String DIRECTIONS_URL = "https://www.google.com/maps/dir/?api=1&destination=";
    private static final String TEL_SCHEME = "tel";

    private ActivityActiveJobBinding binding;
    private ActiveJobViewModel viewModel;
    @Nullable
    private JobMap jobMap;
    @Nullable
    private Location riderPosition;
    @Nullable
    private CurrentLocationProvider locationProvider;

    /**
     * @param context any context
     * @param jobId   the accepted job
     * @return the intent
     */
    @NonNull
    public static Intent intent(@NonNull Context context, long jobId) {
        return new Intent(context, ActiveJobActivity.class).putExtra(EXTRA_JOB_ID, jobId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForMapScreen(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        long jobId = getIntent().getLongExtra(EXTRA_JOB_ID, NO_JOB);
        if (app.sessions().getActive() == null || jobId == NO_JOB) {
            finish();
            return;
        }
        binding = ActivityActiveJobBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(ActiveJobViewModel.class,
                () -> new ActiveJobViewModel(app.riderRepository(), jobId))).get(ActiveJobViewModel.class);
        applyInsets();

        binding.back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.navigate.setOnClickListener(v -> navigate());
        binding.call.setOnClickListener(v -> call());
        binding.action.setOnClickListener(v -> viewModel.completeStep());
        binding.backToBookings.setOnClickListener(v -> {
            startActivity(RiderHomeActivity.intent(this, RiderTab.AVAILABLE));
            finish();
        });
        setUpMap();

        viewModel.getJob().observe(this, this::render);
        viewModel.getBusy().observe(this, busy -> binding.action.setEnabled(!Boolean.TRUE.equals(busy)));
        viewModel.getError().observe(this, this::renderError);
        viewModel.getSessionExpired().observe(this, event -> {
            if (event != null && event.take() != null) {
                RiderLocationService.stop(this);
                app.authRepository().signOut();
                startActivity(new Intent(this, SignInActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) {
            locateRider();
        }
    }

    @Override
    protected void onPause() {
        if (locationProvider != null) {
            locationProvider.cancel();
        }
        super.onPause();
    }

    private void applyInsets() {
        ViewGroup.MarginLayoutParams back = (ViewGroup.MarginLayoutParams) binding.back.getLayoutParams();
        int backTop = back.topMargin;
        int footerBottom = binding.footer.getPaddingBottom();
        int doneTop = binding.done.getPaddingTop();
        int doneBottom = binding.done.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            back.topMargin = backTop + bars.top;
            binding.back.setLayoutParams(back);
            binding.footer.setPadding(binding.footer.getPaddingLeft(), binding.footer.getPaddingTop(),
                    binding.footer.getPaddingRight(), footerBottom + bars.bottom);
            binding.done.setPadding(binding.done.getPaddingLeft(), doneTop + bars.top,
                    binding.done.getPaddingRight(), doneBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ------------------------------------------------------------------ rendering

    private void render(@Nullable RiderJob job) {
        if (job == null) {
            return;
        }
        boolean done = job.isCompleted();
        binding.loading.setVisibility(View.GONE);
        binding.job.setVisibility(done ? View.GONE : View.VISIBLE);
        binding.done.setVisibility(done ? View.VISIBLE : View.GONE);
        if (done) {
            renderDone(job);
        } else {
            renderJob(job);
        }
    }

    private void renderJob(RiderJob job) {
        JobType type = job.getType();
        JobFormat.badge(binding.badge, type);
        binding.meta.setText(JobFormat.meta(this, job));
        binding.initials.setText(job.getCustomerInitials());
        binding.customer.setText(job.getCustomerName());
        binding.address.setText(job.getAddress());
        binding.address.setVisibility(job.getAddress().isEmpty() ? View.GONE : View.VISIBLE);

        List<String> titles = new ArrayList<>();
        for (JobStep step : job.getSteps()) {
            titles.add(step.title());
        }
        binding.steps.setSteps(titles, job.getCurrentStepIndex());

        // OOP: Polymorphism — each JobType words the payment and decides whether cash is collected.
        boolean cash = type.collectsCash(job.getPaymentMethod());
        @ColorRes int fill = cash ? R.color.warn_bg : R.color.success_bg;
        @ColorRes int text = cash ? R.color.warn_text : R.color.success_text;
        ViewCompat.setBackgroundTintList(binding.payBox, ColorStateList.valueOf(ContextCompat.getColor(this, fill)));
        binding.payNote.setText(type.paymentNote(job.getPaymentMethod()));
        binding.payNote.setTextColor(ContextCompat.getColor(this, text));
        binding.payAmount.setText(PesoFormat.amount(job.getTotal()));
        binding.payAmount.setTextColor(ContextCompat.getColor(this, text));

        binding.action.setText(job.currentActionLabel());
        updateMap();
    }

    private void renderDone(RiderJob job) {
        binding.doneSubtitle.setText(getString(R.string.job_done_sub, job.getOrderNo(),
                getString(job.getType().getLabel()), job.getCustomerName()));
        binding.doneNote.setText(job.getType().doneNote(job.getPaymentMethod()));
        binding.doneTotal.setText(PesoFormat.amount(job.getTotal()));
    }

    private void renderError(@Nullable UiText text) {
        if (text != null && viewModel.getJob().getValue() == null) {
            // Nothing loaded yet: show the sheet so its error banner is visible.
            binding.loading.setVisibility(View.GONE);
            binding.job.setVisibility(View.VISIBLE);
        }
        binding.banner.setVisibility(text == null ? View.GONE : View.VISIBLE);
        binding.banner.setText(text == null ? null : text.resolve(this));
    }

    // ------------------------------------------------------------------ map

    /** Adds the Google map (only where Google Play services exist; otherwise the frame stays plain). */
    private void setUpMap() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (available != ConnectionResult.SUCCESS) {
            binding.mapContainer.setContentDescription(getString(R.string.map_unavailable));
            return;
        }
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentByTag(MAP_TAG);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance(new GoogleMapOptions().mapToolbarEnabled(false));
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.map_container, fragment, MAP_TAG)
                    .commitNow();
        }
        fragment.getMapAsync(map -> {
            jobMap = new JobMap(this, map, binding.mapContainer);
            map.setOnMapLoadedCallback(this::updateMap);
            updateMap();
        });
    }

    private void updateMap() {
        RiderJob job = viewModel.getJob().getValue();
        if (jobMap != null && job != null) {
            jobMap.show(riderPosition, job.nextStop());
        }
    }

    /** One GPS fix for the truck marker (the on-duty service keeps sending fixes to the server). */
    private void locateRider() {
        boolean fine = granted(Manifest.permission.ACCESS_FINE_LOCATION);
        if (!fine && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return;
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            return;
        }
        if (locationProvider == null) {
            locationProvider = new FusedCurrentLocationProvider(this);
        }
        locationProvider.request(fine, new CurrentLocationProvider.Callback() {
            @Override
            public void onLocation(@NonNull Location location) {
                riderPosition = location;
                updateMap();
            }

            @Override
            public void onUnavailable() {
                // Keep the pin only; navigation still works.
            }
        });
    }

    private boolean granted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------------ actions

    /** "Navigate in Google Maps": turn-by-turn to the next stop, else directions in the browser. */
    private void navigate() {
        RiderJob job = viewModel.getJob().getValue();
        Location stop = job == null ? null : job.nextStop();
        if (stop == null) {
            snackbar(R.string.no_navigation_app);
            return;
        }
        String point = stop.getLatitude().toPlainString() + "," + stop.getLongitude().toPlainString();
        Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(NAVIGATION_URI + point)).setPackage(GOOGLE_MAPS_PACKAGE);
        try {
            startActivity(app);
        } catch (ActivityNotFoundException noMapsApp) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DIRECTIONS_URL + Uri.encode(point))));
            } catch (ActivityNotFoundException noBrowser) {
                snackbar(R.string.no_navigation_app);
            }
        }
    }

    /** Opens the dialer with the customer's number (no call permission needed). */
    private void call() {
        RiderJob job = viewModel.getJob().getValue();
        String mobile = job == null ? null : job.getCustomerMobile();
        if (mobile == null || mobile.isBlank()) {
            snackbar(R.string.no_customer_phone);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts(TEL_SCHEME, mobile, null)));
        } catch (ActivityNotFoundException e) {
            snackbar(R.string.no_customer_phone);
        }
    }

    private void snackbar(int text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                .setTextColor(ContextCompat.getColor(this, R.color.surface))
                .show();
    }
}
