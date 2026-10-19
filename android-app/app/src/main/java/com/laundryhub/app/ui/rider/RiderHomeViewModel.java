package com.laundryhub.app.ui.rider;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.rider.RiderRepository;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.model.rider.RiderSummary;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R1/R5 state: header summary, tab, both lists and the accept / decline / duty actions. The list
 * follows source/Rider.dc.html: off duty shows only the off-duty card; on duty shows the selected
 * tab's cards or its empty text.
 */
// OOP: Inheritance — an AndroidX ViewModel (survives rotation).
public class RiderHomeViewModel extends ViewModel {

    /** HTTP status of an expired or missing sign-in. */
    static final int UNAUTHORIZED = 401;

    /** Loads per refresh: summary, available, mine. */
    private static final int LOADS_PER_REFRESH = 3;

    // OOP: Encapsulation — mutable state stays private; the screen observes read-only LiveData.
    private final RiderRepository repository;
    private final MutableLiveData<RiderSummary> summary = new MutableLiveData<>();
    private final MutableLiveData<RiderTab> tab = new MutableLiveData<>(RiderTab.AVAILABLE);
    private final MutableLiveData<List<RiderJob>> available = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<RiderJob>> mine = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Set<Long>> busy = new MutableLiveData<>(Collections.emptySet());
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> dutyPending = new MutableLiveData<>(false);
    private final MutableLiveData<UiText> loadError = new MutableLiveData<>();
    private final MutableLiveData<Event<UiText>> notice = new MutableLiveData<>();
    private final MutableLiveData<Event<Long>> openJob = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> sessionExpired = new MutableLiveData<>();
    private final MediatorLiveData<List<RiderListItem>> items = new MediatorLiveData<>();
    private int pendingLoads;
    private ApiException firstLoadError;

    /**
     * @param repository rider endpoints
     */
    public RiderHomeViewModel(@NonNull RiderRepository repository) {
        this.repository = repository;
        items.addSource(summary, x -> rebuild());
        items.addSource(tab, x -> rebuild());
        items.addSource(available, x -> rebuild());
        items.addSource(mine, x -> rebuild());
        items.addSource(busy, x -> rebuild());
    }

    // ------------------------------------------------------------------ loading

    /**
     * Reloads header and both lists (pull-to-refresh, the 15 s timer, after every action).
     *
     * @param showSpinner whether to show the pull-to-refresh spinner
     */
    public void refresh(boolean showSpinner) {
        if (pendingLoads > 0) {
            if (showSpinner) {
                refreshing.setValue(true);
            }
            return;
        }
        pendingLoads = LOADS_PER_REFRESH;
        firstLoadError = null;
        if (showSpinner) {
            refreshing.setValue(true);
        }
        repository.summary(loaded(summary));
        repository.available(loaded(available));
        repository.mine(loaded(mine));
    }

    private <T> ApiCallback<T> loaded(MutableLiveData<T> target) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull T value) {
                target.setValue(value);
                loadDone();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (firstLoadError == null) {
                    firstLoadError = error;
                }
                loadDone();
            }
        };
    }

    private void loadDone() {
        pendingLoads--;
        if (pendingLoads > 0) {
            return;
        }
        refreshing.setValue(false);
        if (firstLoadError == null) {
            loadError.setValue(null);
        } else if (!expired(firstLoadError)) {
            loadError.setValue(ErrorMapper.bannerFor(firstLoadError));
        }
    }

    // ------------------------------------------------------------------ actions

    /**
     * @param selected the tab the rider picked
     */
    public void selectTab(@NonNull RiderTab selected) {
        if (tab.getValue() != selected) {
            tab.setValue(selected);
        }
    }

    /**
     * The duty switch. The server saves it; the header follows the server's answer.
     *
     * @param onDuty requested position
     */
    public void setDuty(boolean onDuty) {
        if (Boolean.TRUE.equals(dutyPending.getValue())) {
            return;
        }
        dutyPending.setValue(true);
        repository.setDuty(onDuty, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull RiderSummary value) {
                dutyPending.setValue(false);
                summary.setValue(value);
                refresh(false);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                dutyPending.setValue(false);
                if (!expired(error)) {
                    notice.setValue(new Event<>(ErrorMapper.bannerFor(error)));
                }
            }
        });
    }

    /**
     * "Accept booking": opens the active job, or says another rider was first (409).
     *
     * @param job the booking
     */
    public void accept(@NonNull RiderJob job) {
        if (!markBusy(job.getId())) {
            return;
        }
        repository.accept(job.getId(), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull RiderJob accepted) {
                clearBusy(job.getId());
                available.setValue(without(available.getValue(), job.getId()));
                openJob.setValue(new Event<>(accepted.getId()));
                refresh(false);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                clearBusy(job.getId());
                if (expired(error)) {
                    return;
                }
                if (error.getStatus() == RiderRepository.ALREADY_TAKEN) {
                    available.setValue(without(available.getValue(), job.getId()));
                    notice.setValue(new Event<>(UiText.of(R.string.booking_taken)));
                    refresh(false);
                } else {
                    notice.setValue(new Event<>(ErrorMapper.bannerFor(error)));
                }
            }
        });
    }

    /**
     * "Decline": hides the booking for this rider only.
     *
     * @param job the booking
     */
    public void decline(@NonNull RiderJob job) {
        if (!markBusy(job.getId())) {
            return;
        }
        repository.decline(job.getId(), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Boolean done) {
                clearBusy(job.getId());
                available.setValue(without(available.getValue(), job.getId()));
                refresh(false);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                clearBusy(job.getId());
                if (!expired(error)) {
                    notice.setValue(new Event<>(ErrorMapper.bannerFor(error)));
                }
            }
        });
    }

    /**
     * "Start job" / "Continue job".
     *
     * @param job an accepted job
     */
    public void open(@NonNull RiderJob job) {
        openJob.setValue(new Event<>(job.getId()));
    }

    private boolean expired(ApiException error) {
        if (error.getStatus() == UNAUTHORIZED) {
            sessionExpired.setValue(new Event<>(true));
            return true;
        }
        return false;
    }

    private boolean markBusy(long id) {
        Set<Long> now = busy.getValue();
        if (now != null && now.contains(id)) {
            return false;
        }
        Set<Long> next = new HashSet<>(now == null ? Collections.emptySet() : now);
        next.add(id);
        busy.setValue(Collections.unmodifiableSet(next));
        return true;
    }

    private void clearBusy(long id) {
        Set<Long> next = new HashSet<>(busy.getValue() == null ? Collections.emptySet() : busy.getValue());
        next.remove(id);
        busy.setValue(Collections.unmodifiableSet(next));
    }

    private static List<RiderJob> without(List<RiderJob> jobs, long id) {
        List<RiderJob> kept = new ArrayList<>();
        if (jobs != null) {
            for (RiderJob job : jobs) {
                if (job.getId() != id) {
                    kept.add(job);
                }
            }
        }
        return Collections.unmodifiableList(kept);
    }

    // ------------------------------------------------------------------ list

    private void rebuild() {
        RiderSummary header = summary.getValue();
        if (header == null) {
            items.setValue(Collections.emptyList());
            return;
        }
        if (!header.onDuty()) {
            items.setValue(List.of(RiderListItem.State.offDuty(R.string.off_duty_message)));
            return;
        }
        RiderTab selected = tab.getValue() == null ? RiderTab.AVAILABLE : tab.getValue();
        boolean accepted = selected == RiderTab.MY_JOBS;
        List<RiderJob> source = accepted ? mine.getValue() : available.getValue();
        if (source == null || source.isEmpty()) {
            items.setValue(List.of(RiderListItem.State.empty(selected.getEmptyText())));
            return;
        }
        Set<Long> running = busy.getValue() == null ? Collections.emptySet() : busy.getValue();
        List<RiderListItem> rows = new ArrayList<>();
        for (RiderJob job : source) {
            rows.add(new RiderListItem.Booking(job, accepted, running.contains(job.getId())));
        }
        items.setValue(Collections.unmodifiableList(rows));
    }

    // ------------------------------------------------------------------ observed state

    /** @return header: name, duty, counters (also the tab counts) */
    @NonNull
    public LiveData<RiderSummary> getSummary() {
        return summary;
    }

    /** @return selected tab */
    @NonNull
    public LiveData<RiderTab> getTab() {
        return tab;
    }

    /** @return rows for the list */
    @NonNull
    public LiveData<List<RiderListItem>> getItems() {
        return items;
    }

    /** @return pull-to-refresh spinner */
    @NonNull
    public LiveData<Boolean> getRefreshing() {
        return refreshing;
    }

    /** @return whether the duty switch is being saved */
    @NonNull
    public LiveData<Boolean> getDutyPending() {
        return dutyPending;
    }

    /** @return the banner when loading failed, {@code null} when the last refresh worked */
    @NonNull
    public LiveData<UiText> getLoadError() {
        return loadError;
    }

    /** @return one-time messages (booking taken, action failed) */
    @NonNull
    public LiveData<Event<UiText>> getNotice() {
        return notice;
    }

    /** @return job to open in the active job screen */
    @NonNull
    public LiveData<Event<Long>> getOpenJob() {
        return openJob;
    }

    /** @return fired when the server says the sign-in expired */
    @NonNull
    public LiveData<Event<Boolean>> getSessionExpired() {
        return sessionExpired;
    }
}
