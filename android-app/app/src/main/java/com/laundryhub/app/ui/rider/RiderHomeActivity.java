package com.laundryhub.app.ui.rider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ActivityRiderHomeBinding;
import com.laundryhub.app.databinding.ViewCounterTileBinding;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.model.rider.RiderSummary;
import com.laundryhub.app.ui.auth.SignInActivity;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * R1-rider-bookings / R5-rider-off-duty: the rider's home. Refreshes every 15 s while visible and
 * on pull-to-refresh. Going on duty asks for location and starts {@link RiderLocationService}.
 */
// OOP: Inheritance — an AppCompatActivity.
public class RiderHomeActivity extends AppCompatActivity implements BookingAdapter.Listener {

    /** Intent extra: which tab to show (a {@link RiderTab} name). */
    public static final String EXTRA_TAB = "com.laundryhub.app.extra.RIDER_TAB";

    /** Auto refresh while the screen is visible. */
    public static final long AUTO_REFRESH_MS = 15_000L;

    private ActivityRiderHomeBinding binding;
    private RiderHomeViewModel viewModel;
    private BookingAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            viewModel.refresh(false);
            handler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };
    private boolean locationHintShown;

    /** Location (and on Android 13+ notifications) before going on duty; on duty either way. */
    private final ActivityResultLauncher<String[]> dutyPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> viewModel.setDuty(true));

    /**
     * @param context any context
     * @param tab     the tab to show
     * @return an intent that brings the existing rider home to the front
     */
    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull RiderTab tab) {
        return new Intent(context, RiderHomeActivity.class)
                .putExtra(EXTRA_TAB, tab.name())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForBlueHeader(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        if (app.sessions().getActive() == null) {
            backToSignIn();
            return;
        }
        binding = ActivityRiderHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(RiderHomeViewModel.class,
                () -> new RiderHomeViewModel(app.riderRepository()))).get(RiderHomeViewModel.class);
        applyInsets();

        counter(binding.countAvailable, R.string.counter_available);
        counter(binding.countAccepted, R.string.counter_accepted);
        counter(binding.countDone, R.string.counter_done_today);
        binding.tabs.setSegments(new CharSequence[]{tabLabel(RiderTab.AVAILABLE, 0), tabLabel(RiderTab.MY_JOBS, 0)});
        binding.tabs.setOnSegmentSelectedListener(index -> viewModel.selectTab(RiderTab.values()[index]));
        binding.duty.setOnToggleListener(this::onDutyToggle);
        binding.chat.setOnClickListener(v -> startActivity(com.laundryhub.app.ui.feedback.ChatActivity.intent(this)));

        adapter = new BookingAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.addItemDecoration(new BookingAdapter.Spacing(this));
        binding.list.setAdapter(adapter);
        if (binding.list.getItemAnimator() instanceof SimpleItemAnimator animator) {
            // The 15 s refresh rebinds cards; no cross-fade flicker.
            animator.setSupportsChangeAnimations(false);
        }
        binding.refresh.setColorSchemeResources(R.color.accent);
        binding.refresh.setOnRefreshListener(() -> viewModel.refresh(true));

        viewModel.getSummary().observe(this, this::renderSummary);
        viewModel.getTab().observe(this, tab -> binding.tabs.setSelectedIndex(tab.ordinal()));
        viewModel.getItems().observe(this, adapter::submitList);
        viewModel.getRefreshing().observe(this, binding.refresh::setRefreshing);
        viewModel.getDutyPending().observe(this, pending -> binding.duty.setEnabled(!Boolean.TRUE.equals(pending)));
        viewModel.getLoadError().observe(this, this::renderBanner);
        viewModel.getNotice().observe(this, event -> {
            UiText text = event == null ? null : event.take();
            if (text != null) {
                snackbar(text.resolve(this));
            }
        });
        viewModel.getOpenJob().observe(this, event -> {
            Long id = event == null ? null : event.take();
            if (id != null) {
                startActivity(ActiveJobActivity.intent(this, id));
            }
        });
        viewModel.getSessionExpired().observe(this, event -> {
            if (event != null && event.take() != null) {
                signOut(app);
            }
        });
        showTab(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showTab(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            handler.post(autoRefresh);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(autoRefresh);
        super.onPause();
    }

    private void showTab(Intent intent) {
        String name = intent.getStringExtra(EXTRA_TAB);
        if (name != null) {
            viewModel.selectTab(RiderTab.fromName(name));
        }
    }

    private void applyInsets() {
        int headerTop = binding.header.getPaddingTop();
        int listBottom = binding.list.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            binding.header.setPadding(binding.header.getPaddingLeft(), headerTop + bars.top,
                    binding.header.getPaddingRight(), binding.header.getPaddingBottom());
            binding.list.setPadding(binding.list.getPaddingLeft(), binding.list.getPaddingTop(),
                    binding.list.getPaddingRight(), listBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private static void counter(ViewCounterTileBinding tile, @StringRes int label) {
        tile.label.setText(label);
        tile.value.setText(String.valueOf(0));
    }

    private String tabLabel(RiderTab tab, long count) {
        return getString(tab.getLabel(), (int) count);
    }

    // ------------------------------------------------------------------ rendering

    private void renderSummary(@Nullable RiderSummary summary) {
        if (summary == null) {
            return;
        }
        binding.greeting.setText(getString(R.string.rider_greeting, summary.riderName()));
        binding.duty.setOnDuty(summary.onDuty());
        binding.countAvailable.value.setText(String.valueOf(summary.availableCount()));
        binding.countAccepted.value.setText(String.valueOf(summary.acceptedCount()));
        binding.countDone.value.setText(String.valueOf(summary.doneTodayCount()));
        setCounterDescription(binding.countAvailable);
        setCounterDescription(binding.countAccepted);
        setCounterDescription(binding.countDone);
        List<String> labels = new ArrayList<>();
        labels.add(tabLabel(RiderTab.AVAILABLE, summary.availableCount()));
        labels.add(tabLabel(RiderTab.MY_JOBS, summary.acceptedCount()));
        for (int i = 0; i < labels.size(); i++) {
            binding.tabs.getSegmentViews().get(i).setText(labels.get(i));
        }
        syncLocationService(summary.onDuty());
    }

    private static void setCounterDescription(ViewCounterTileBinding tile) {
        tile.getRoot().setContentDescription(tile.label.getText() + " " + tile.value.getText());
    }

    private void renderBanner(@Nullable UiText text) {
        binding.banner.setVisibility(text == null ? View.GONE : View.VISIBLE);
        binding.banner.setText(text == null ? null : text.resolve(this));
    }

    // ------------------------------------------------------------------ duty & GPS

    private void onDutyToggle(boolean onDuty) {
        if (onDuty && !RiderLocationService.hasLocationPermission(this)) {
            dutyPermissions.launch(dutyPermissionNames());
        } else {
            viewModel.setDuty(onDuty);
        }
    }

    private static String[] dutyPermissionNames() {
        List<String> names = new ArrayList<>(List.of(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            names.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return names.toArray(new String[0]);
    }

    /** On duty → GPS service running (if allowed); off duty → stopped. */
    private void syncLocationService(boolean onDuty) {
        if (!onDuty) {
            RiderLocationService.stop(this);
        } else if (RiderLocationService.hasLocationPermission(this)) {
            RiderLocationService.start(this);
        } else if (!locationHintShown) {
            locationHintShown = true;
            snackbar(getString(R.string.duty_location_needed));
        }
    }

    // ------------------------------------------------------------------ card buttons

    @Override
    public void onAccept(@NonNull RiderJob job) {
        viewModel.accept(job);
    }

    @Override
    public void onDecline(@NonNull RiderJob job) {
        viewModel.decline(job);
    }

    @Override
    public void onOpen(@NonNull RiderJob job) {
        viewModel.open(job);
    }

    // ------------------------------------------------------------------ helpers

    private void snackbar(CharSequence text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                .setTextColor(ContextCompat.getColor(this, R.color.surface))
                .show();
    }

    private void signOut(AppContainer app) {
        RiderLocationService.stop(this);
        app.authRepository().signOut();
        backToSignIn();
    }

    private void backToSignIn() {
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
