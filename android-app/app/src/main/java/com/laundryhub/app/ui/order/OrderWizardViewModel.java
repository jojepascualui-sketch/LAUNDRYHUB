package com.laundryhub.app.ui.order;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.dto.ShopDto;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.PinCheck;
import com.laundryhub.app.model.PlacedOrder;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.model.Soap;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.Debouncer;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

import java.util.Collections;
import java.util.List;

/**
 * The order being built, shared by every wizard step (activity-scoped). Same state and rules as
 * source/Main.dc.html: service, kilos (never below the minimum), soap, time chip, pin, payment.
 * The footer total is a local estimate at once and the server's {@code POST /api/orders/quote}
 * answer as soon as it arrives.
 */
// OOP: Inheritance — an Android ViewModel that survives rotation.
public class OrderWizardViewModel extends ViewModel {

    /** Minimum load used until {@code GET /api/pricing} answers (design default). */
    public static final int DEFAULT_MIN_KG = 8;

    // OOP: Encapsulation — screens observe read-only LiveData and call the actions below.
    private final OrderRepository repository;
    private final Debouncer quoteDebouncer;

    private final MutableLiveData<WizardStep> step = new MutableLiveData<>(WizardStep.SERVICE);
    private final MutableLiveData<OrderDraft> draft = new MutableLiveData<>(OrderDraft.start(DEFAULT_MIN_KG, null));
    private final MutableLiveData<Pricing> pricing = new MutableLiveData<>();
    private final MutableLiveData<ShopDto> shop = new MutableLiveData<>();
    private final MutableLiveData<List<SlotDto>> slots = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<PriceLines> price = new MutableLiveData<>();
    private final MutableLiveData<UiText> loadError = new MutableLiveData<>();
    private final MutableLiveData<UiText> pinError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkingPin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> placing = new MutableLiveData<>(false);
    private final MutableLiveData<UiText> placeError = new MutableLiveData<>();
    private final MutableLiveData<PlacedOrder> placedOrder = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> exit = new MutableLiveData<>();
    private final MediatorLiveData<Boolean> canContinue = new MediatorLiveData<>(false);

    private boolean loadingCatalog;
    private int quoteRequest;
    private int pinRequest;
    /** Last address filled in from the pin; replaced on the next pin only if the user did not edit it. */
    @NonNull
    private String autoFilledAddress = "";

    /**
     * @param repository     server calls
     * @param quoteDebouncer waits for the stepper to settle before asking for a quote
     */
    public OrderWizardViewModel(@NonNull OrderRepository repository, @NonNull Debouncer quoteDebouncer) {
        this.repository = repository;
        this.quoteDebouncer = quoteDebouncer;
        canContinue.addSource(step, s -> updateCanContinue());
        canContinue.addSource(draft, d -> updateCanContinue());
        canContinue.addSource(placing, p -> updateCanContinue());
        canContinue.addSource(pricing, p -> updateCanContinue());
    }

    // ---------------------------------------------------------------- state

    /** @return current step */
    @NonNull
    public LiveData<WizardStep> getStep() {
        return step;
    }

    /** @return the order so far */
    @NonNull
    public LiveData<OrderDraft> getDraft() {
        return draft;
    }

    /** @return prices and soaps, {@code null} until loaded */
    @NonNull
    public LiveData<Pricing> getPricing() {
        return pricing;
    }

    /** @return the shop card (04b) */
    @NonNull
    public LiveData<ShopDto> getShop() {
        return shop;
    }

    /** @return time chips for the chosen service */
    @NonNull
    public LiveData<List<SlotDto>> getSlots() {
        return slots;
    }

    /** @return price lines: local estimate, then the server quote */
    @NonNull
    public LiveData<PriceLines> getPrice() {
        return price;
    }

    /** @return why prices could not be loaded, or {@code null} */
    @NonNull
    public LiveData<UiText> getLoadError() {
        return loadError;
    }

    /** @return why the pin cannot be used (outside the delivery radius), or {@code null} */
    @NonNull
    public LiveData<UiText> getPinError() {
        return pinError;
    }

    /** @return whether the pin is being checked with the server */
    @NonNull
    public LiveData<Boolean> getCheckingPin() {
        return checkingPin;
    }

    /** @return whether "Place order" is running */
    @NonNull
    public LiveData<Boolean> getPlacing() {
        return placing;
    }

    /** @return why the order could not be placed, or {@code null} */
    @NonNull
    public LiveData<UiText> getPlaceError() {
        return placeError;
    }

    /** @return the placed order (06), {@code null} before */
    @NonNull
    public LiveData<PlacedOrder> getPlacedOrder() {
        return placedOrder;
    }

    /** @return fires when the wizard should close (back from step 1, Back to home) */
    @NonNull
    public LiveData<Event<Boolean>> getExit() {
        return exit;
    }

    /** @return whether Continue / Place order is enabled */
    @NonNull
    public LiveData<Boolean> getCanContinue() {
        return canContinue;
    }

    /** @return whether the − button is disabled (kilos at the minimum) */
    public boolean isAtMinimum() {
        return current().getKg() <= minKg();
    }

    // ---------------------------------------------------------------- loading

    /** Loads prices, soaps and the shop once (call from the screen). */
    public void start() {
        if (pricing.getValue() == null && !loadingCatalog) {
            loadCatalog();
        }
    }

    /** "Try again" after a failed load. */
    public void retry() {
        if (!loadingCatalog) {
            loadCatalog();
        }
    }

    private void loadCatalog() {
        loadingCatalog = true;
        loadError.setValue(null);
        repository.loadPricing(new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Pricing value) {
                loadingCatalog = false;
                pricing.setValue(value);
                OrderDraft d = current();
                int kg = Math.max(d.getKg(), value.minKgWhole());
                OrderDraft updated = d.withKg(kg);
                if ((d.getSoapId() == null || value.soap(d.getSoapId()) == null) && !value.getSoaps().isEmpty()) {
                    updated = updated.withSoap(value.getSoaps().get(0).id());
                }
                draft.setValue(updated);
                priceInputsChanged();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingCatalog = false;
                loadError.setValue(ErrorMapper.bannerFor(error));
            }
        });
        repository.loadShop(new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull ShopDto value) {
                shop.setValue(value);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                // The shop card simply stays empty; the order can still be placed.
            }
        });
    }

    // ---------------------------------------------------------------- actions (prototype buttons)

    /**
     * Service card tapped. A different service clears the time chip and loads its chips.
     *
     * @param service the service
     */
    public void selectService(@NonNull ServiceType service) {
        OrderDraft d = current();
        if (d.getService() == service) {
            return;
        }
        draft.setValue(d.withService(service));
        slots.setValue(Collections.emptyList());
        repository.loadSlots(service, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull List<SlotDto> value) {
                if (current().getService() == service) {
                    slots.setValue(value);
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadError.setValue(ErrorMapper.bannerFor(error));
            }
        });
        priceInputsChanged();
    }

    /** "+" on the stepper. */
    public void increaseKg() {
        draft.setValue(current().withKg(current().getKg() + 1));
        priceInputsChanged();
    }

    /** "−" on the stepper; stops at the minimum load (the button is disabled there). */
    public void decreaseKg() {
        if (isAtMinimum()) {
            return;
        }
        draft.setValue(current().withKg(current().getKg() - 1));
        priceInputsChanged();
    }

    /**
     * Soap card tapped.
     *
     * @param soapId the soap
     */
    public void selectSoap(long soapId) {
        draft.setValue(current().withSoap(soapId));
        priceInputsChanged();
    }

    /**
     * Time chip tapped.
     *
     * @param slot the chip
     */
    public void selectSlot(@NonNull SlotDto slot) {
        draft.setValue(current().withSlot(new OrderDraft.Slot(slot.getDate(), slot.getLabel(), slot.getText())));
    }

    /**
     * The pin moved (map tap, marker drag or "Use my current location"). The backend reverse-geocodes
     * it and checks the delivery radius; the address fills "Address details" unless the customer has
     * typed their own, and a pin outside the radius blocks Continue with an error.
     *
     * @param pin the pinned location
     */
    public void setPin(@NonNull Location pin) {
        if (pin.equals(current().getPin())) {
            return;
        }
        draft.setValue(current().withPin(pin));
        pinError.setValue(null);
        checkingPin.setValue(true);
        int request = ++pinRequest;
        repository.checkPin(pin, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull PinCheck check) {
                if (request == pinRequest) {
                    applyPinCheck(check);
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (request == pinRequest) {
                    // Unknown: the server checks the pin again when the order is placed.
                    checkingPin.setValue(false);
                }
            }
        });
    }

    private void applyPinCheck(@NonNull PinCheck check) {
        checkingPin.setValue(false);
        OrderDraft d = current();
        if (check.isKnown()) {
            d = d.withPinCheck(check.inside());
            pinError.setValue(check.inside() ? null : UiText.format(R.string.pin_outside_radius,
                    PesoFormat.number(check.distanceKm()), PesoFormat.number(check.radiusKm())));
        }
        String details = d.getAddressDetails().trim();
        if (check.address() != null && (details.isEmpty() || details.equals(autoFilledAddress))) {
            autoFilledAddress = check.address();
            d = d.withAddressDetails(check.address());
        }
        draft.setValue(d);
    }

    /**
     * @param details "Address details" as typed
     */
    public void setAddressDetails(@NonNull String details) {
        if (!details.equals(current().getAddressDetails())) {
            draft.setValue(current().withAddressDetails(details));
        }
    }

    /**
     * Cash / GCash card tapped.
     *
     * @param option the method
     */
    public void selectPayment(@NonNull PaymentOption option) {
        draft.setValue(current().withPayment(option));
        placeError.setValue(null);
    }

    /**
     * @param number "GCash mobile number" as typed
     */
    public void setGcashNumber(@NonNull String number) {
        if (!number.equals(current().getGcashNumber())) {
            draft.setValue(current().withGcashNumber(number));
        }
    }

    /** Continue, or Place order on the last step. Ignored while disabled. */
    public void next() {
        if (!Boolean.TRUE.equals(canContinue.getValue())) {
            return;
        }
        WizardStep s = currentStep();
        if (s.isLast()) {
            placeOrder();
        } else {
            step.setValue(s.next());
        }
    }

    /** Back button: previous step, or leave the wizard from step 1. */
    public void back() {
        if (Boolean.TRUE.equals(placing.getValue())) {
            return;
        }
        if (placedOrder.getValue() != null) {
            exit.setValue(new Event<>(true));
            return;
        }
        WizardStep previous = currentStep().previous();
        if (previous == null) {
            exit.setValue(new Event<>(true));
        } else {
            step.setValue(previous);
        }
    }

    /** "Back to home" on 06. */
    public void finish() {
        exit.setValue(new Event<>(true));
    }

    // ---------------------------------------------------------------- price and order

    /** Shows a local estimate now and asks the server once the inputs settle. */
    private void priceInputsChanged() {
        Pricing p = pricing.getValue();
        OrderDraft d = current();
        Soap soap = p == null || d.getSoapId() == null ? null : p.soap(d.getSoapId());
        if (p == null || soap == null) {
            return;
        }
        price.setValue(p.estimate(d.getService(), d.getKg(), soap));
        quoteDebouncer.submit(this::requestQuote);
    }

    private void requestQuote() {
        OrderDraft d = current();
        if (d.getSoapId() == null) {
            return;
        }
        int request = ++quoteRequest;
        // Before a service is chosen the prototype shows the total without a fee: quote it as walk-in.
        ServiceType service = d.getService() == null ? ServiceType.WALK_IN : d.getService();
        repository.quote(service, d.getKg(), d.getSoapId(), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull PriceLines value) {
                if (request == quoteRequest) {
                    price.setValue(value);
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                // Keep the local estimate; the server checks the price again when the order is placed.
            }
        });
    }

    private void placeOrder() {
        placing.setValue(true);
        placeError.setValue(null);
        repository.placeOrder(current(), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull PlacedOrder value) {
                placing.setValue(false);
                placedOrder.setValue(value);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                placing.setValue(false);
                placeError.setValue(ErrorMapper.bannerFor(error));
            }
        });
    }

    private void updateCanContinue() {
        boolean busy = Boolean.TRUE.equals(placing.getValue());
        boolean loaded = pricing.getValue() != null;
        // OOP: Polymorphism — WizardStep.isComplete is answered by the current step.
        canContinue.setValue(!busy && loaded && currentStep().isComplete(current()));
    }

    private int minKg() {
        Pricing p = pricing.getValue();
        return p == null ? DEFAULT_MIN_KG : p.minKgWhole();
    }

    @NonNull
    private OrderDraft current() {
        OrderDraft d = draft.getValue();
        return d == null ? OrderDraft.start(DEFAULT_MIN_KG, null) : d;
    }

    @NonNull
    private WizardStep currentStep() {
        WizardStep s = step.getValue();
        return s == null ? WizardStep.SERVICE : s;
    }

    @Override
    protected void onCleared() {
        quoteDebouncer.cancel();
    }

    /** @return text for "Placing order…" */
    @NonNull
    public static UiText placingLabel() {
        return UiText.of(R.string.placing_order);
    }

    /**
     * @return a soap by id from the loaded prices, or {@code null}
     * @param id soap id
     */
    @Nullable
    public Soap soap(long id) {
        Pricing p = pricing.getValue();
        return p == null ? null : p.soap(id);
    }
}
