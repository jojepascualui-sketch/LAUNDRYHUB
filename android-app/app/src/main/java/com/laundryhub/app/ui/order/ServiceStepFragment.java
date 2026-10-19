package com.laundryhub.app.ui.order;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.FragmentStepServiceBinding;
import com.laundryhub.app.databinding.ViewServiceCardBinding;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;

/** Step 1 · 02-choose-service: Walk-in and Pickup &amp; Delivery cards. */
// OOP: Inheritance — a WizardStepFragment.
public class ServiceStepFragment extends WizardStepFragment {

    private FragmentStepServiceBinding binding;

    /** Required empty constructor. */
    public ServiceStepFragment() {
        super(R.layout.fragment_step_service);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentStepServiceBinding.bind(view);
        OrderWizardViewModel vm = viewModel();
        bindCard(binding.walkIn, ServiceType.WALK_IN, vm);
        bindCard(binding.delivery, ServiceType.PICKUP_DELIVERY, vm);
        vm.getDraft().observe(getViewLifecycleOwner(), this::renderSelection);
        vm.getPricing().observe(getViewLifecycleOwner(), this::renderFees);
    }

    private void bindCard(ViewServiceCardBinding card, ServiceType service, OrderWizardViewModel vm) {
        card.icon.setImageResource(service.getIcon());
        card.name.setText(service.getDisplayName());
        card.description.setText(service.getDescription());
        card.getRoot().setOnClickListener(v -> vm.selectService(service));
    }

    private void renderSelection(@NonNull OrderDraft draft) {
        binding.walkIn.getRoot().setChecked(draft.getService() == ServiceType.WALK_IN);
        binding.delivery.getRoot().setChecked(draft.getService() == ServiceType.PICKUP_DELIVERY);
    }

    private void renderFees(@Nullable Pricing pricing) {
        if (pricing == null) {
            return;
        }
        // OOP: Polymorphism — each ServiceType writes its own fee line.
        binding.walkIn.fee.setText(ServiceType.WALK_IN.feeLabel(pricing).resolve(requireContext()));
        binding.delivery.fee.setText(ServiceType.PICKUP_DELIVERY.feeLabel(pricing).resolve(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
