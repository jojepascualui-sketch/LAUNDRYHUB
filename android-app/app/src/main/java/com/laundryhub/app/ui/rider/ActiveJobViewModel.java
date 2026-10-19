package com.laundryhub.app.ui.rider;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.rider.RiderRepository;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

/**
 * R2/R3/R4: one accepted job. The big button completes the current step ("Cash received" on the
 * cash step); when the last step is done the job is completed and the screen shows R4.
 */
// OOP: Inheritance — an AndroidX ViewModel.
public class ActiveJobViewModel extends ViewModel {

    /** The server answers 409 when the job moved on (e.g. the cash step is due). */
    private static final int CONFLICT = 409;

    // OOP: Encapsulation — mutable state stays private.
    private final RiderRepository repository;
    private final long jobId;
    private final MutableLiveData<RiderJob> job = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<UiText> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> sessionExpired = new MutableLiveData<>();

    /**
     * Loads the job right away.
     *
     * @param repository rider endpoints
     * @param jobId      the job to show
     */
    public ActiveJobViewModel(@NonNull RiderRepository repository, long jobId) {
        this.repository = repository;
        this.jobId = jobId;
        load();
    }

    /** Reloads the job from the server. */
    public void load() {
        repository.job(jobId, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull RiderJob value) {
                job.setValue(value);
                error.setValue(null);
            }

            @Override
            public void onError(@NonNull ApiException e) {
                fail(e);
            }
        });
    }

    /** The big footer button: completes the current step. */
    public void completeStep() {
        RiderJob current = job.getValue();
        if (current == null || current.isCompleted() || Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        error.setValue(null);
        repository.completeStep(current, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull RiderJob value) {
                busy.setValue(false);
                job.setValue(value);
            }

            @Override
            public void onError(@NonNull ApiException e) {
                busy.setValue(false);
                fail(e);
                if (e.getStatus() == CONFLICT) {
                    load();
                }
            }
        });
    }

    private void fail(ApiException e) {
        if (e.getStatus() == RiderHomeViewModel.UNAUTHORIZED) {
            sessionExpired.setValue(new Event<>(true));
        } else {
            error.setValue(ErrorMapper.bannerFor(e));
        }
    }

    /** @return the job ({@code null} until loaded) */
    @NonNull
    public LiveData<RiderJob> getJob() {
        return job;
    }

    /** @return whether a step is being saved (button disabled) */
    @NonNull
    public LiveData<Boolean> getBusy() {
        return busy;
    }

    /** @return the error banner text, or {@code null} */
    @NonNull
    public LiveData<UiText> getError() {
        return error;
    }

    /** @return fired when the server says the sign-in expired */
    @NonNull
    public LiveData<Event<Boolean>> getSessionExpired() {
        return sessionExpired;
    }
}
