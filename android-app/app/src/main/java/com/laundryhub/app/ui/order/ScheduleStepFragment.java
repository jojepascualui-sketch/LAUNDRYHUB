package com.laundryhub.app.ui.order;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.R;
import com.laundryhub.app.data.api.dto.ShopDto;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.databinding.FragmentStepScheduleBinding;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.order.location.CurrentLocationProvider;
import com.laundryhub.app.ui.order.location.FusedCurrentLocationProvider;
import com.laundryhub.app.ui.order.location.PinMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 3 · 04a-pin-location-delivery (hint, Google map with the orange pin, status chip,
 * "Use my current location", address details, pickup chips) or 04b-drop-off-walk-in (shop card,
 * drop-off chips).
 */
// OOP: Inheritance — a WizardStepFragment.
public class ScheduleStepFragment extends WizardStepFragment {

    /** Chips per row. */
    public static final int CHIP_COLUMNS = 2;

    private static final String MAP_TAG = "pin_map";
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private FragmentStepScheduleBinding binding;
    private final List<SlotChip> chips = new ArrayList<>();
    @Nullable
    private PinMap pinMap;
    @Nullable
    private CurrentLocationProvider locationProvider;
    private boolean cameraPlaced;

    /** Asks for location permission, then fetches the fix. */
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    /** A chip and the slot it stands for. */
    private record SlotChip(SlotDto slot, MaterialButton view) {
    }

    /** Required empty constructor. */
    public ScheduleStepFragment() {
        super(R.layout.fragment_step_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentStepScheduleBinding.bind(view);
        OrderWizardViewModel vm = viewModel();
        OrderDraft draft = vm.getDraft().getValue();
        if (draft != null) {
            binding.address.setText(draft.getAddressDetails());
        }
        binding.address.getInput().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel().setAddressDetails(s.toString());
            }
        });
        binding.currentLocation.setOnClickListener(v -> useCurrentLocation());
        locationProvider = new FusedCurrentLocationProvider(requireContext());
        setUpMap();

        vm.getShop().observe(getViewLifecycleOwner(), this::renderShop);
        vm.getSlots().observe(getViewLifecycleOwner(), this::buildChips);
        vm.getDraft().observe(getViewLifecycleOwner(), this::renderDraft);
        vm.getPinError().observe(getViewLifecycleOwner(), this::renderPinError);
    }

    // ------------------------------------------------------------------ map

    /** Adds the Google map to the frame (only where Google Play services exist). */
    private void setUpMap() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (available != ConnectionResult.SUCCESS) {
            // No map: the frame stays empty and "Use my current location" still works.
            binding.map.setContentDescription(getString(R.string.map_unavailable));
            return;
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentByTag(MAP_TAG);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance(new GoogleMapOptions().mapToolbarEnabled(false));
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment, MAP_TAG)
                    .commitNow();
        }
        mapFragment.getMapAsync(map -> {
            if (binding == null) {
                return;
            }
            pinMap = new PinMap(requireContext(), map, location -> viewModel().setPin(location));
            OrderDraft draft = viewModel().getDraft().getValue();
            pinMap.show(draft == null ? null : draft.getPin());
            placeCamera(false);
        });
    }

    /** Opens on the pin, else on the shop; once the shop position arrives it is used too. */
    private void placeCamera(boolean animate) {
        if (pinMap == null) {
            return;
        }
        OrderDraft draft = viewModel().getDraft().getValue();
        Location pin = draft == null ? null : draft.getPin();
        pinMap.focus(pin, shopLocation(viewModel().getShop().getValue()), animate);
        cameraPlaced = pin != null || viewModel().getShop().getValue() != null;
    }

    @Nullable
    private static Location shopLocation(@Nullable ShopDto shop) {
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) {
            return null;
        }
        return new Location(shop.getLatitude(), shop.getLongitude());
    }

    // ------------------------------------------------------------------ "Use my current location"

    private void useCurrentLocation() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            fetchCurrentLocation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(binding.getRoot(), R.string.location_permission_rationale, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.ink))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.surface))
                    .setAction(R.string.allow, v -> permissionLauncher.launch(LOCATION_PERMISSIONS))
                    .show();
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS);
        }
    }

    private void onPermissionResult(Map<String, Boolean> granted) {
        if (Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            fetchCurrentLocation();
            return;
        }
        // Denied. If Android will not ask again, offer the app settings.
        boolean blocked = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.location_permission_denied, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.ink))
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.surface));
        if (blocked) {
            snackbar.setAction(R.string.open_settings, v -> startActivity(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireContext().getPackageName(), null))));
        }
        snackbar.show();
    }

    private void fetchCurrentLocation() {
        if (locationProvider == null) {
            return;
        }
        boolean precise = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        locationProvider.request(precise, new CurrentLocationProvider.Callback() {
            @Override
            public void onLocation(@NonNull Location location) {
                if (binding == null) {
                    return;
                }
                viewModel().setPin(location);
                if (pinMap != null) {
                    pinMap.focus(location, null, true);
                }
            }

            @Override
            public void onUnavailable() {
                if (binding != null) {
                    message(R.string.location_unavailable);
                }
            }
        });
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void message(@StringRes int text) {
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.ink))
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.surface))
                .show();
    }

    // ------------------------------------------------------------------ rendering

    private void renderShop(@Nullable ShopDto shop) {
        if (shop == null) {
            return;
        }
        binding.shopTitle.setText(getString(R.string.drop_off_at, shop.getName()));
        binding.shopDetails.setText(shop.getAddress() + "\n" + getString(R.string.shop_open, shop.getHoursLabel()));
        if (!cameraPlaced) {
            placeCamera(false);
        }
    }

    private void buildChips(@NonNull List<SlotDto> slots) {
        binding.slotGrid.removeAllViews();
        chips.clear();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = getResources().getDimensionPixelSize(R.dimen.grid_gap);
        LinearLayout row = null;
        for (int i = 0; i < slots.size(); i++) {
            if (i % CHIP_COLUMNS == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                binding.slotGrid.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            SlotDto slot = slots.get(i);
            MaterialButton chip = (MaterialButton) inflater.inflate(R.layout.item_slot_chip, row, false);
            chip.setText(slot.getText());
            chip.setOnClickListener(v -> viewModel().selectSlot(slot));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
            if (i % CHIP_COLUMNS == 0) {
                params.setMarginEnd(gap / 2);
            } else {
                params.setMarginStart(gap - gap / 2);
            }
            row.addView(chip, params);
            chips.add(new SlotChip(slot, chip));
        }
        if (row != null && slots.size() % CHIP_COLUMNS != 0) {
            row.addView(new Space(requireContext()), new LinearLayout.LayoutParams(0, 0, 1f));
        }
        OrderDraft draft = viewModel().getDraft().getValue();
        if (draft != null) {
            renderDraft(draft);
        }
    }

    private void renderDraft(@NonNull OrderDraft draft) {
        ServiceType service = draft.getService() == null ? ServiceType.WALK_IN : draft.getService();
        // OOP: Polymorphism — the service decides whether a pin is needed and how the chips are titled.
        boolean delivery = service.requiresPin();
        binding.deliveryBlock.setVisibility(delivery ? View.VISIBLE : View.GONE);
        binding.shopCard.setVisibility(delivery ? View.GONE : View.VISIBLE);
        binding.slotTitle.setText(service.slotTitle());
        binding.pinStatus.setText(draft.getPin() == null ? R.string.tap_to_pin : R.string.location_pinned);
        if (pinMap != null) {
            pinMap.show(draft.getPin());
        }
        // Reflect an address filled in from the pin, without fighting the customer's typing.
        String typed = binding.address.getInput().getText() == null ? "" : binding.address.getInput().getText().toString();
        if (!typed.equals(draft.getAddressDetails())) {
            binding.address.setText(draft.getAddressDetails());
            binding.address.getInput().setSelection(draft.getAddressDetails().length());
        }
        OrderDraft.Slot chosen = draft.getSlot();
        for (SlotChip chip : chips) {
            chip.view().setChecked(chosen != null && chosen.date().equals(chip.slot().getDate())
                    && chosen.label().equals(chip.slot().getLabel()));
        }
    }

    private void renderPinError(@Nullable UiText error) {
        binding.pinError.setText(error == null ? null : error.resolve(requireContext()));
        binding.pinError.setVisibility(error == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationProvider != null) {
            locationProvider.cancel();
        }
        locationProvider = null;
        pinMap = null;
        cameraPlaced = false;
        chips.clear();
        binding = null;
    }
}
