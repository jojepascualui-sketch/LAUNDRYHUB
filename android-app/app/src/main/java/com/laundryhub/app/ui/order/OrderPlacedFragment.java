package com.laundryhub.app.ui.order;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.FragmentOrderPlacedBinding;
import com.laundryhub.app.databinding.ItemSummaryRowBinding;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.PlacedOrder;
import com.laundryhub.app.ui.common.SystemBars;

/** 06-order-placed: numbers come from the server's response. */
// OOP: Inheritance — a WizardStepFragment (shares the wizard ViewModel).
public class OrderPlacedFragment extends WizardStepFragment {

    private FragmentOrderPlacedBinding binding;

    /** Required empty constructor. */
    public OrderPlacedFragment() {
        super(R.layout.fragment_order_placed);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderPlacedBinding.bind(view);
        SystemBars.padForSystemBars(binding.root);
        binding.rowTotal.amount.setTextAppearance(R.style.TextAppearance_LaundryHub_Total_Medium);
        binding.backHome.setOnClickListener(v -> viewModel().finish());
        viewModel().getPlacedOrder().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable PlacedOrder order) {
        if (order == null) {
            return;
        }
        binding.subtitle.setText(getString(R.string.order_placed_sub, order.orderNo(), order.serviceName()));
        row(binding.rowWeight, R.string.summary_weight, getString(R.string.kg_amount, PesoFormat.number(order.weightKg())));
        row(binding.rowSoap, R.string.fabric_soap, order.soapName());
        row(binding.rowSchedule, R.string.summary_schedule, order.scheduleText());
        row(binding.rowPayment, R.string.summary_payment, order.paymentName());
        binding.rowTotal.amount.setText(PesoFormat.amount(order.total()));
    }

    private static void row(ItemSummaryRowBinding row, int label, String value) {
        row.label.setText(label);
        row.value.setText(value);
        row.value.setTextAppearance(R.style.TextAppearance_LaundryHub_Strong);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
