package com.laundryhub.app.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

/** 01 Home: the "₱30 /kg" and "Minimum load 8 kg" tiles come from {@code GET /api/pricing}. */
// OOP: Inheritance — an Android ViewModel.
public class HomeViewModel extends ViewModel {

    // OOP: Encapsulation — read-only LiveData for the screen.
    private final OrderRepository repository;
    private final MutableLiveData<Pricing> pricing = new MutableLiveData<>();
    private final MutableLiveData<Event<UiText>> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Long>> openTracking = new MutableLiveData<>();
    private final MutableLiveData<Event<UiText>> notice = new MutableLiveData<>();
    private boolean loading;
    private boolean findingOrder;

    /**
     * @param repository server calls
     */
    public HomeViewModel(@NonNull OrderRepository repository) {
        this.repository = repository;
    }

    /** @return prices, {@code null} until loaded */
    @NonNull
    public LiveData<Pricing> getPricing() {
        return pricing;
    }

    /** @return one-time load errors */
    @NonNull
    public LiveData<Event<UiText>> getError() {
        return error;
    }

    /** @return order to open in 07 Track order */
    @NonNull
    public LiveData<Event<Long>> getOpenTracking() {
        return openTracking;
    }

    /** @return one-time messages (no orders yet, load failed) */
    @NonNull
    public LiveData<Event<UiText>> getNotice() {
        return notice;
    }

    /** Orders tab: the Orders list has no design (CLAUDE.md §7), so it opens Track order for the newest order. */
    public void openLatestOrder() {
        if (findingOrder) {
            return;
        }
        findingOrder = true;
        repository.myOrders(new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull java.util.List<com.laundryhub.app.model.tracking.OrderSummary> orders) {
                findingOrder = false;
                if (orders.isEmpty()) {
                    notice.setValue(new Event<>(UiText.of(com.laundryhub.app.R.string.no_orders_yet)));
                } else {
                    openTracking.setValue(new Event<>(orders.get(0).id()));
                }
            }

            @Override
            public void onError(@NonNull ApiException e) {
                findingOrder = false;
                notice.setValue(new Event<>(ErrorMapper.bannerFor(e)));
            }
        });
    }

    /** Loads prices (again on each return to Home, so admin changes show up). */
    public void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        repository.loadPricing(new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Pricing value) {
                loading = false;
                pricing.setValue(value);
            }

            @Override
            public void onError(@NonNull ApiException e) {
                loading = false;
                error.setValue(new Event<>(ErrorMapper.bannerFor(e)));
            }
        });
    }
}
