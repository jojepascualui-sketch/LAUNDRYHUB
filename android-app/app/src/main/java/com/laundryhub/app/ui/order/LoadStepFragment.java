package com.laundryhub.app.ui.order;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.FragmentStepLoadBinding;
import com.laundryhub.app.databinding.ItemSoapCardBinding;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.Soap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Step 2 · 03-weight-and-soap: kilo stepper (− disabled at the minimum) and the 2×2 soap grid. */
// OOP: Inheritance — a WizardStepFragment.
public class LoadStepFragment extends WizardStepFragment {

    /** Soap cards per row. */
    public static final int SOAP_COLUMNS = 2;

    private FragmentStepLoadBinding binding;
    private final Map<Long, ItemSoapCardBinding> soapCards = new LinkedHashMap<>();

    /** Required empty constructor. */
    public LoadStepFragment() {
        super(R.layout.fragment_step_load);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentStepLoadBinding.bind(view);
        OrderWizardViewModel vm = viewModel();
        binding.minus.setOnClickListener(v -> vm.decreaseKg());
        binding.plus.setOnClickListener(v -> vm.increaseKg());
        vm.getPricing().observe(getViewLifecycleOwner(), this::renderPricing);
        vm.getDraft().observe(getViewLifecycleOwner(), this::renderDraft);
    }

    private void renderPricing(@Nullable Pricing pricing) {
        if (pricing == null) {
            return;
        }
        binding.rate.setText(getString(R.string.rate_per_kg, PesoFormat.compact(pricing.getRatePerKg())));
        binding.minimumNote.setText(getString(R.string.minimum_note, PesoFormat.number(pricing.getMinKg())));
        buildSoapGrid(pricing.getSoaps());
        OrderDraft draft = viewModel().getDraft().getValue();
        if (draft != null) {
            renderDraft(draft);
        }
    }

    private void buildSoapGrid(List<Soap> soaps) {
        binding.soapGrid.removeAllViews();
        soapCards.clear();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = getResources().getDimensionPixelSize(R.dimen.grid_gap);
        LinearLayout row = null;
        for (int i = 0; i < soaps.size(); i++) {
            if (i % SOAP_COLUMNS == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                binding.soapGrid.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            Soap soap = soaps.get(i);
            ItemSoapCardBinding card = ItemSoapCardBinding.inflate(inflater, row, false);
            card.name.setText(soap.name());
            card.note.setText(soap.description());
            card.addOn.setText(soap.isIncluded() ? getString(R.string.soap_included)
                    : getString(R.string.soap_add_on, PesoFormat.compact(soap.addOnPerKg())));
            card.tile.setBackgroundTintList(ColorStateList.valueOf(tint(soap)));
            card.getRoot().setOnClickListener(v -> viewModel().selectSoap(soap.id()));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getRoot().getLayoutParams();
            if (i % SOAP_COLUMNS == 0) {
                params.setMarginEnd(gap / 2);
            } else {
                params.setMarginStart(gap - gap / 2);
            }
            row.addView(card.getRoot(), params);
            soapCards.put(soap.id(), card);
        }
        if (row != null && soaps.size() % SOAP_COLUMNS != 0) {
            row.addView(new Space(requireContext()), new LinearLayout.LayoutParams(0, 0, 1f));
        }
    }

    /** The soap's tile color from the server, or accent tint if it is not a valid color. */
    private int tint(Soap soap) {
        try {
            return Color.parseColor(soap.tintHex());
        } catch (IllegalArgumentException e) {
            return ContextCompat.getColor(requireContext(), R.color.accent_tint);
        }
    }

    private void renderDraft(@NonNull OrderDraft draft) {
        binding.kg.setText(String.valueOf(draft.getKg()));
        binding.minus.setEnabled(!viewModel().isAtMinimum());
        for (Map.Entry<Long, ItemSoapCardBinding> entry : soapCards.entrySet()) {
            entry.getValue().getRoot().setChecked(entry.getKey().equals(draft.getSoapId()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        soapCards.clear();
        binding = null;
    }
}
