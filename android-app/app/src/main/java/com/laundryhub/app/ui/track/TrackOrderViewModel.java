package com.laundryhub.app.ui.track;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.feedback.FeedbackRepository;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.model.feedback.Rating;
import com.laundryhub.app.ui.common.Event;

import java.util.Optional;
import com.laundryhub.app.model.tracking.Tracking;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.UiText;

/**
 * 07 Track order: loads {@code GET /api/orders/{id}/tracking}; the screen polls {@link #refresh()} every few
 * seconds so the rider marker and timeline stay live.
 */
// OOP: Inheritance — an AndroidX ViewModel.
public class TrackOrderViewModel extends ViewModel {

    // OOP: Encapsulation — mutable state stays private.
    private final OrderRepository repository;
    private final FeedbackRepository feedback;
    private final MutableLiveData<Optional<Rating>> rating = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ratingSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Event<UiText>> ratingError = new MutableLiveData<>();
    private boolean ratingRequested;
    private final long orderId;
    private final MutableLiveData<Tracking> tracking = new MutableLiveData<>();
    private final MutableLiveData<UiText> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private boolean inFlight;

    /**
     * @param repository order endpoints
     * @param feedback   order ratings
     * @param orderId    the order to track
     */
    public TrackOrderViewModel(@NonNull OrderRepository repository, @NonNull FeedbackRepository feedback,
                               long orderId) {
        this.repository = repository;
        this.feedback = feedback;
        this.orderId = orderId;
        refresh();
    }

    /** Reloads the tracking data (ignored while a call is running). */
    public void refresh() {
        if (inFlight) {
            return;
        }
        inFlight = true;
        repository.tracking(orderId, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Tracking value) {
                inFlight = false;
                loading.setValue(false);
                error.setValue(null);
                tracking.setValue(value);
                if (value.isCompleted() && !ratingRequested) {
                    loadRating();
                }
            }

            @Override
            public void onError(@NonNull ApiException e) {
                inFlight = false;
                loading.setValue(false);
                error.setValue(ErrorMapper.bannerFor(e));
            }
        });
    }

    private void loadRating() {
        ratingRequested = true;
        feedback.rating(orderId, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Optional<Rating> value) {
                rating.setValue(value);
            }

            @Override
            public void onError(@NonNull ApiException e) {
                ratingRequested = false;
            }
        });
    }

    /**
     * "Send rating" (completed orders only; no design, CLAUDE.md §7).
     *
     * @param stars   1–5
     * @param comment optional comment
     */
    public void rate(int stars, String comment) {
        Tracking current = tracking.getValue();
        if (current == null || !current.isCompleted() || Boolean.TRUE.equals(ratingSaving.getValue())) {
            return;
        }
        Rating value;
        try {
            value = new Rating(stars, comment);
        } catch (IllegalArgumentException e) {
            return;
        }
        ratingSaving.setValue(true);
        feedback.rate(orderId, value, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Rating saved) {
                ratingSaving.setValue(false);
                rating.setValue(Optional.of(saved));
            }

            @Override
            public void onError(@NonNull ApiException e) {
                ratingSaving.setValue(false);
                ratingError.setValue(new Event<>(ErrorMapper.bannerFor(e)));
            }
        });
    }

    /** @return the rating once a completed order was checked: empty = not rated yet */
    @NonNull
    public LiveData<Optional<Rating>> getRating() {
        return rating;
    }

    /** @return whether "Send rating" is running */
    @NonNull
    public LiveData<Boolean> getRatingSaving() {
        return ratingSaving;
    }

    /** @return one-time rating errors */
    @NonNull
    public LiveData<Event<UiText>> getRatingError() {
        return ratingError;
    }

    /** @return tracking data, {@code null} until loaded */
    @NonNull
    public LiveData<Tracking> getTracking() {
        return tracking;
    }

    /** @return error banner text, or {@code null} */
    @NonNull
    public LiveData<UiText> getError() {
        return error;
    }

    /** @return whether the first load is still running */
    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }
}
