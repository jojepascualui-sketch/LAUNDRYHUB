package com.laundryhub.app.ui.track;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ActivityTrackOrderBinding;
import com.laundryhub.app.databinding.ItemTrackStepBinding;
import com.laundryhub.app.model.tracking.Tracking;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;

import java.util.List;

/**
 * 07-track-order: map with shop, live rider and pin, "Your rider" card with Call, and the five-step
 * order status. Polls the server every {@value #POLL_MS} ms while visible.
 */
// OOP: Inheritance — an AppCompatActivity.
public class TrackOrderActivity extends AppCompatActivity {

    /** Intent extra: the order id. */
    public static final String EXTRA_ORDER_ID = "com.laundryhub.app.extra.ORDER_ID";
    /** Refresh interval while the screen is open. */
    public static final long POLL_MS = 5_000L;

    private static final long NO_ORDER = -1L;
    private static final String MAP_TAG = "track_map";
    private static final String TEL_SCHEME = "tel";

    private ActivityTrackOrderBinding binding;
    private TrackOrderViewModel viewModel;
    @Nullable
    private TrackMap trackMap;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            viewModel.refresh();
            handler.postDelayed(this, POLL_MS);
        }
    };

    /**
     * @param context any context
     * @param orderId the order to track
     * @return the intent
     */
    @NonNull
    public static Intent intent(@NonNull Context context, long orderId) {
        return new Intent(context, TrackOrderActivity.class).putExtra(EXTRA_ORDER_ID, orderId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForLightScreen(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        long orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, NO_ORDER);
        if (app.sessions().getActive() == null || orderId == NO_ORDER) {
            finish();
            return;
        }
        binding = ActivityTrackOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.padForSystemBars(binding.getRoot());
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(TrackOrderViewModel.class,
                () -> new TrackOrderViewModel(app.orderRepository(), app.feedbackRepository(), orderId)))
                .get(TrackOrderViewModel.class);

        binding.back.setOnClickListener(v -> finish());
        binding.call.setOnClickListener(v -> call());
        setUpMap();
        viewModel.getTracking().observe(this, this::render);
        viewModel.getLoading().observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.loading.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.content.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        });
        viewModel.getError().observe(this, this::renderError);

        binding.messageShop.setOnClickListener(
                v -> startActivity(com.laundryhub.app.ui.feedback.ChatActivity.intent(this)));
        buildStars();
        binding.ratingSend.setOnClickListener(v -> viewModel.rate(chosenStars,
                binding.ratingComment.getText() == null ? null : binding.ratingComment.getText().toString()));
        viewModel.getRating().observe(this, this::renderRating);
        viewModel.getRatingSaving().observe(this, saving -> binding.ratingSend.setEnabled(
                !Boolean.TRUE.equals(saving) && chosenStars > 0));
        viewModel.getRatingError().observe(this, event -> {
            UiText text = event == null ? null : event.take();
            if (text != null) {
                Snackbar.make(binding.getRoot(), text.resolve(this), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                        .setTextColor(ContextCompat.getColor(this, R.color.surface))
                        .show();
            }
        });
    }

    private int chosenStars;

    /** Five 44 dp star buttons (no design: accent when chosen, input border outline otherwise). */
    private void buildStars() {
        int touch = getResources().getDimensionPixelSize(R.dimen.star_touch);
        for (int stars = com.laundryhub.app.model.feedback.Rating.MIN_STARS;
             stars <= com.laundryhub.app.model.feedback.Rating.MAX_STARS; stars++) {
            android.widget.ImageButton star = new android.widget.ImageButton(this);
            star.setLayoutParams(new android.widget.LinearLayout.LayoutParams(touch, touch));
            star.setBackground(null);
            star.setContentDescription(getString(R.string.rate_star, stars));
            int value = stars;
            star.setOnClickListener(v -> chooseStars(value));
            binding.stars.addView(star);
        }
        chooseStars(0);
    }

    private void chooseStars(int stars) {
        chosenStars = stars;
        for (int i = 0; i < binding.stars.getChildCount(); i++) {
            ((android.widget.ImageButton) binding.stars.getChildAt(i)).setImageResource(
                    i < stars ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }
        binding.ratingSend.setEnabled(stars > 0 && !Boolean.TRUE.equals(viewModel.getRatingSaving().getValue()));
    }

    private void renderRating(@Nullable java.util.Optional<com.laundryhub.app.model.feedback.Rating> rating) {
        Tracking tracking = viewModel.getTracking().getValue();
        boolean show = rating != null && tracking != null && tracking.isCompleted();
        binding.ratingCard.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        if (rating.isPresent()) {
            chooseStars(rating.get().stars());
            if (binding.ratingComment.getText() == null || binding.ratingComment.getText().length() == 0) {
                binding.ratingComment.setText(rating.get().comment());
            }
            binding.ratingThanks.setText(getString(R.string.rate_thanks, rating.get().stars()));
            binding.ratingThanks.setVisibility(View.VISIBLE);
            binding.ratingSend.setText(R.string.rate_update);
        } else {
            binding.ratingThanks.setVisibility(View.GONE);
            binding.ratingSend.setText(R.string.rate_send);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            handler.postDelayed(poll, POLL_MS);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(poll);
        super.onPause();
    }

    private void render(@Nullable Tracking tracking) {
        if (tracking == null) {
            return;
        }
        binding.orderNumber.setText(getString(R.string.order_number, tracking.getOrderNo()));
        binding.liveChip.setVisibility(tracking.isLive() ? View.VISIBLE : View.GONE);
        binding.cancelled.setVisibility(tracking.isCancelled() ? View.VISIBLE : View.GONE);

        Tracking.Rider rider = tracking.getRider();
        binding.riderCard.setVisibility(rider == null ? View.GONE : View.VISIBLE);
        if (rider != null) {
            binding.riderInitials.setText(rider.initials());
            binding.riderName.setText(rider.name());
            binding.riderSub.setText(getString(R.string.your_rider, rider.plate()));
        }
        renderSteps(tracking.getRows());
        if (trackMap != null) {
            trackMap.show(tracking);
        }
    }

    private void renderSteps(List<Tracking.Row> rows) {
        binding.steps.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int lineRow = getResources().getDimensionPixelSize(R.dimen.track_dot)
                + getResources().getDimensionPixelSize(R.dimen.track_line_min);
        for (int i = 0; i < rows.size(); i++) {
            Tracking.Row row = rows.get(i);
            ItemTrackStepBinding item = ItemTrackStepBinding.inflate(inflater, binding.steps, false);
            boolean done = row.state() == Tracking.State.DONE;
            boolean reached = row.state() != Tracking.State.UPCOMING;
            boolean hasLine = i < rows.size() - 1;
            item.dot.setBackgroundResource(done ? R.drawable.bg_step_done
                    : reached ? R.drawable.bg_step_current : R.drawable.bg_step_upcoming);
            item.check.setVisibility(done ? View.VISIBLE : View.GONE);
            item.line.setVisibility(hasLine ? View.VISIBLE : View.GONE);
            item.line.setBackgroundColor(ContextCompat.getColor(this, done ? R.color.accent : R.color.border));
            item.title.setText(row.step().getTitle());
            item.title.setTextColor(ContextCompat.getColor(this, reached ? R.color.ink : R.color.subtle));
            item.note.setText(row.step().getNote());
            item.getRoot().setMinimumHeight(hasLine ? lineRow : 0);
            item.getRoot().setContentDescription(getString(row.step().getTitle()) + ", " + getString(row.step().getNote())
                    + (done ? ", " + getString(R.string.state_done) : ""));
            binding.steps.addView(item.getRoot());
        }
    }

    private void renderError(@Nullable UiText text) {
        binding.banner.setVisibility(text == null ? View.GONE : View.VISIBLE);
        binding.banner.setText(text == null ? null : text.resolve(this));
    }

    /** Adds the Google map (only where Google Play services exist). */
    private void setUpMap() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            binding.map.setContentDescription(getString(R.string.map_unavailable));
            return;
        }
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentByTag(MAP_TAG);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance(new GoogleMapOptions().mapToolbarEnabled(false));
            getSupportFragmentManager().beginTransaction().replace(R.id.map_container, fragment, MAP_TAG).commitNow();
        }
        fragment.getMapAsync(map -> {
            trackMap = new TrackMap(this, map, binding.map);
            Tracking current = viewModel.getTracking().getValue();
            if (current != null) {
                trackMap.show(current);
            }
        });
    }

    /** "Call rider": opens the dialer. */
    private void call() {
        Tracking tracking = viewModel.getTracking().getValue();
        String mobile = tracking == null || tracking.getRider() == null ? null : tracking.getRider().mobile();
        if (mobile == null || mobile.isBlank()) {
            snackbar(R.string.no_rider_phone);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts(TEL_SCHEME, mobile, null)));
        } catch (ActivityNotFoundException e) {
            snackbar(R.string.no_rider_phone);
        }
    }

    private void snackbar(int text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                .setTextColor(ContextCompat.getColor(this, R.color.surface))
                .show();
    }
}
