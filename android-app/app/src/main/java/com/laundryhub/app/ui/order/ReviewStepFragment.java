package com.laundryhub.app.ui.order;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.FragmentStepReviewBinding;
import com.laundryhub.app.databinding.ViewPaymentCardBinding;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.ui.auth.AuthValidator;
import com.laundryhub.app.ui.common.UiText;

/** Step 4 · 05-review-and-pay: order summary (server quote), Cash / GCash, GCash number. */
// OOP: Inheritance — a WizardStepFragment.
public class ReviewStepFragment extends WizardStepFragment {

    private FragmentStepReviewBinding binding;

    /** Required empty constructor. */
    public ReviewStepFragment() {
        super(R.layout.fragment_step_review);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentStepReviewBinding.bind(view);
        OrderWizardViewModel vm = viewModel();
        binding.rowService.label.setText(R.string.summary_service);
        binding.rowService.value.setTextAppearance(R.style.TextAppearance_LaundryHub_Strong);
        binding.rowFee.label.setText(R.string.summary_delivery_fee);
        bindPayment(binding.cash, PaymentOption.CASH, vm);
        bindPayment(binding.gcash, PaymentOption.GCASH, vm);
        OrderDraft draft = vm.getDraft().getValue();
        if (draft != null) {
            binding.gcashNumber.setText(draft.getGcashNumber());
        }
        binding.gcashNumber.getInput().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel().setGcashNumber(s.toString());
            }
        });
        vm.getDraft().observe(getViewLifecycleOwner(), this::renderDraft);
        vm.getPrice().observe(getViewLifecycleOwner(), this::renderPrice);
        vm.getPricing().observe(getViewLifecycleOwner(), this::renderMethods);
        vm.getPlaceError().observe(getViewLifecycleOwner(), this::renderError);
    }

    private void bindPayment(ViewPaymentCardBinding card, PaymentOption option, OrderWizardViewModel vm) {
        card.name.setText(option.getDisplayName());
        card.badge.setText(option.getBadge());
        card.badge.setTextColor(ContextCompat.getColor(requireContext(), option.getBadgeColor()));
        card.tile.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), option.getTileColor())));
        card.getRoot().setOnClickListener(v -> vm.selectPayment(option));
    }

    private void renderDraft(@NonNull OrderDraft draft) {
        ServiceType service = draft.getService();
        binding.rowService.value.setText(service == null ? "" : getString(service.getDisplayName()));
        // OOP: Polymorphism — PaymentOption.description depends on the method and the service.
        binding.cash.description.setText(PaymentOption.CASH.description(service));
        binding.gcash.description.setText(PaymentOption.GCASH.description(service));
        binding.cash.getRoot().setChecked(draft.getPayment() == PaymentOption.CASH);
        binding.gcash.getRoot().setChecked(draft.getPayment() == PaymentOption.GCASH);
        boolean gcash = draft.getPayment().requiresMobileNumber();
        binding.gcashNumber.setVisibility(gcash ? View.VISIBLE : View.GONE);
        String number = AuthValidator.normalizeMobile(draft.getGcashNumber());
        boolean complete = number.length() >= AuthValidator.MOBILE_LENGTH;
        binding.gcashNumber.setError(gcash && complete && !AuthValidator.isMobile(number)
                ? getString(R.string.error_mobile) : null);
    }

    private void renderPrice(@Nullable PriceLines price) {
        if (price == null) {
            return;
        }
        String kg = PesoFormat.number(price.billableKg());
        binding.rowLaundry.label.setText(getString(R.string.summary_laundry, kg, PesoFormat.compact(price.ratePerKg())));
        binding.rowLaundry.value.setText(PesoFormat.amount(price.laundryCost()));
        binding.rowSoap.label.setText(getString(R.string.summary_soap, price.soapName(), kg,
                PesoFormat.compact(price.soapAddOnPerKg())));
        binding.rowSoap.value.setText(PesoFormat.amount(price.soapCost()));
        binding.rowFee.value.setText(PesoFormat.amount(price.deliveryFee()));
        binding.rowTotal.amount.setText(PesoFormat.amount(price.total()));
    }

    /** A method the admin switched off (A3) is not offered. */
    private void renderMethods(@Nullable Pricing pricing) {
        if (pricing == null) {
            return;
        }
        binding.cash.getRoot().setVisibility(pricing.isEnabled(PaymentOption.CASH) ? View.VISIBLE : View.GONE);
        binding.gcash.getRoot().setVisibility(pricing.isEnabled(PaymentOption.GCASH) ? View.VISIBLE : View.GONE);
    }

    private void renderError(@Nullable UiText error) {
        binding.errorBanner.setText(error == null ? null : error.resolve(requireContext()));
        binding.errorBanner.setVisibility(error == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
