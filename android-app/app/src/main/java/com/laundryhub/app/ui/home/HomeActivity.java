package com.laundryhub.app.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.databinding.ActivityHomeBinding;
import com.laundryhub.app.databinding.ViewHowStepBinding;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.ui.auth.SignInActivity;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;
import com.laundryhub.app.ui.order.OrderWizardActivity;

/**
 * 01-home: greeting, "Start laundry order", price tiles, "How it works" and the bottom nav.
 * Orders, Profile and the bell have no design yet (CLAUDE.md §7).
 */
// OOP: Inheritance — an AppCompatActivity.
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForLightScreen(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        if (app.sessions().getActive() == null) {
            backToSignIn();
            return;
        }
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(HomeViewModel.class,
                () -> new HomeViewModel(app.orderRepository()))).get(HomeViewModel.class);
        applyInsets();

        howStep(binding.how1, 1, R.string.how_step_1);
        howStep(binding.how2, 2, R.string.how_step_2);
        howStep(binding.how3, 3, R.string.how_step_3);
        binding.nav.navHome.setSelected(true);
        binding.nav.navHome.setTypeface(binding.nav.navHome.getTypeface(), android.graphics.Typeface.BOLD);

        binding.startOrder.setOnClickListener(v -> startActivity(new Intent(this, OrderWizardActivity.class)));
        binding.bell.setOnClickListener(v -> message(R.string.notifications_unavailable));
        binding.nav.navOrders.setOnClickListener(v -> viewModel.openLatestOrder());
        viewModel.getOpenTracking().observe(this, event -> {
            Long orderId = event == null ? null : event.take();
            if (orderId != null) {
                startActivity(com.laundryhub.app.ui.track.TrackOrderActivity.intent(this, orderId));
            }
        });
        viewModel.getNotice().observe(this, event -> {
            UiText text = event == null ? null : event.take();
            if (text != null) {
                Snackbar.make(binding.getRoot(), text.resolve(this), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                        .setTextColor(ContextCompat.getColor(this, R.color.surface))
                        .show();
            }
        });
        binding.nav.navProfile.setOnClickListener(v -> showProfile(app));

        viewModel.getPricing().observe(this, this::renderPricing);
        viewModel.getError().observe(this, event -> {
            UiText text = event == null ? null : event.take();
            if (text != null) {
                Snackbar.make(binding.getRoot(), text.resolve(this), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                        .setTextColor(ContextCompat.getColor(this, R.color.surface))
                        .setAction(R.string.try_again, a -> viewModel.refresh())
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void applyInsets() {
        int contentTop = binding.content.getPaddingTop();
        int navBottom = binding.nav.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.content.setPadding(binding.content.getPaddingLeft(), contentTop + bars.top,
                    binding.content.getPaddingRight(), binding.content.getPaddingBottom());
            binding.nav.getRoot().setPadding(0, binding.nav.getRoot().getPaddingTop(), 0, navBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private static void howStep(ViewHowStepBinding row, int number, @StringRes int text) {
        row.number.setText(String.valueOf(number));
        row.text.setText(text);
    }

    private void renderPricing(@Nullable Pricing pricing) {
        if (pricing == null) {
            return;
        }
        binding.rate.setText(PesoFormat.compact(pricing.getRatePerKg()));
        binding.minimum.setText(getString(R.string.kg_amount, PesoFormat.number(pricing.getMinKg())));
    }

    private void message(@StringRes int text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                .setTextColor(ContextCompat.getColor(this, R.color.surface))
                .show();
    }

    /** Profile has no design yet: a dialog with the account name and Sign out. */
    private void showProfile(AppContainer app) {
        Session session = app.sessions().getActive();
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.profile_title, session == null ? "" : session.getFullName()))
                .setPositiveButton(R.string.sign_out, (d, w) -> {
                    app.authRepository().signOut();
                    backToSignIn();
                })
                .setNegativeButton(R.string.ok, null)
                // Feedback chat has no design (CLAUDE.md §7): reached from Profile.
                .setNeutralButton(R.string.chat_title,
                        (d, w) -> startActivity(com.laundryhub.app.ui.feedback.ChatActivity.intent(this)))
                .show();
    }

    private void backToSignIn() {
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
