package com.laundryhub.app.ui.order;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ActivityOrderWizardBinding;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.MainThreadDebouncer;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.ViewModelFactory;

/**
 * Order wizard 02–05 and 06: header (back, "Step N of 4", title, progress), the current step and
 * the sticky footer ("Estimated total" + Continue / Place order). Owns the shared ViewModel.
 */
// OOP: Inheritance — an AppCompatActivity hosting the step fragments.
public class OrderWizardActivity extends AppCompatActivity {

    /** Wait this long after the last stepper tap before asking the server for a quote. */
    public static final long QUOTE_DEBOUNCE_MILLIS = 250;

    private static final String DONE_TAG = "DONE";

    private ActivityOrderWizardBinding binding;
    private OrderWizardViewModel viewModel;

    /**
     * @param context any context of this app
     * @return the factory for the shared wizard ViewModel
     */
    @NonNull
    public static ViewModelProvider.Factory viewModelFactory(@NonNull Context context) {
        return new ViewModelFactory<>(OrderWizardViewModel.class, () -> new OrderWizardViewModel(
                LaundryHubApp.container(context).orderRepository(), new MainThreadDebouncer(QUOTE_DEBOUNCE_MILLIS)));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForLightScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivityOrderWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this, viewModelFactory(this)).get(OrderWizardViewModel.class);
        applyInsets();

        binding.back.setOnClickListener(v -> viewModel.back());
        binding.next.setOnClickListener(v -> viewModel.next());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.back();
            }
        });

        viewModel.getStep().observe(this, this::renderStep);
        viewModel.getDraft().observe(this, this::renderTitle);
        viewModel.getPrice().observe(this, this::renderTotal);
        viewModel.getCanContinue().observe(this, enabled -> binding.next.setEnabled(Boolean.TRUE.equals(enabled)));
        viewModel.getPlacing().observe(this, placing -> renderButtonLabel());
        viewModel.getPlacedOrder().observe(this, order -> {
            if (order != null) {
                showDone();
            }
        });
        viewModel.getExit().observe(this, this::onExit);
        viewModel.start();
    }

    /** Header under the status bar; footer above the navigation bar and the keyboard. */
    private void applyInsets() {
        int headerTop = binding.header.getPaddingTop();
        int footerBottom = binding.footer.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            binding.header.setPadding(binding.header.getPaddingLeft(), headerTop + bars.top,
                    binding.header.getPaddingRight(), binding.header.getPaddingBottom());
            binding.footer.setPadding(binding.footer.getPaddingLeft(), binding.footer.getPaddingTop(),
                    binding.footer.getPaddingRight(), footerBottom + Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }

    private void renderStep(@NonNull WizardStep step) {
        if (viewModel.getPlacedOrder().getValue() != null) {
            return;
        }
        binding.stepLabel.setText(getString(R.string.step_n_of_4, step.number()));
        LinearLayout.LayoutParams fill = (LinearLayout.LayoutParams) binding.progressFill.getLayoutParams();
        fill.weight = step.number();
        binding.progressFill.setLayoutParams(fill);
        renderTitle(viewModel.getDraft().getValue());
        renderButtonLabel();
        Fragment shown = getSupportFragmentManager().findFragmentById(R.id.step_container);
        if (shown == null || !step.name().equals(shown.getTag())) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.step_container, step.createScreen(), step.name())
                    .commit();
        }
    }

    private void renderTitle(@Nullable OrderDraft draft) {
        WizardStep step = viewModel.getStep().getValue();
        if (step != null) {
            // OOP: Polymorphism — each WizardStep names itself (step 3 depends on the service).
            binding.stepTitle.setText(step.title(draft == null ? null : draft.getService()));
        }
    }

    private void renderTotal(@Nullable PriceLines price) {
        binding.total.setText(price == null ? null : PesoFormat.amount(price.total()));
    }

    private void renderButtonLabel() {
        WizardStep step = viewModel.getStep().getValue();
        boolean placing = Boolean.TRUE.equals(viewModel.getPlacing().getValue());
        if (placing) {
            binding.next.setText(OrderWizardViewModel.placingLabel().resolve(this));
        } else {
            binding.next.setText(step != null && step.isLast() ? R.string.place_order : R.string.continue_button);
        }
    }

    /** 06 takes the whole screen. */
    private void showDone() {
        binding.header.setVisibility(View.GONE);
        binding.footer.setVisibility(View.GONE);
        if (getSupportFragmentManager().findFragmentByTag(DONE_TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.step_container, new OrderPlacedFragment(), DONE_TAG)
                    .commit();
        }
    }

    private void onExit(@Nullable Event<Boolean> event) {
        if (event != null && event.take() != null) {
            finish();
        }
    }
}
