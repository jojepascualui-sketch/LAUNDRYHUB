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
    private boolean loading;

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
