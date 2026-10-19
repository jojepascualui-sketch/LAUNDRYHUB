package com.laundryhub.app.ui.feedback;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.feedback.FeedbackRepository;
import com.laundryhub.app.model.feedback.ChatMessage;
import com.laundryhub.app.ui.auth.ErrorMapper;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Feedback chat with the shop (customers and riders). The screen polls {@link #refresh()} every few seconds;
 * only messages newer than the last one are fetched.
 */
// OOP: Inheritance — an AndroidX ViewModel.
public class ChatViewModel extends ViewModel {

    // OOP: Encapsulation — mutable state stays private.
    private final FeedbackRepository repository;
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<UiText> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> sent = new MutableLiveData<>();
    private boolean inFlight;

    /**
     * Loads the conversation right away.
     *
     * @param repository feedback endpoints
     */
    public ChatViewModel(@NonNull FeedbackRepository repository) {
        this.repository = repository;
        refresh();
    }

    /** Fetches messages newer than the last one shown (ignored while a call is running). */
    public void refresh() {
        if (inFlight) {
            return;
        }
        inFlight = true;
        repository.messages(lastId(), new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull List<ChatMessage> value) {
                inFlight = false;
                loading.setValue(false);
                error.setValue(null);
                append(value);
            }

            @Override
            public void onError(@NonNull ApiException e) {
                inFlight = false;
                loading.setValue(false);
                error.setValue(ErrorMapper.bannerFor(e));
            }
        });
    }

    /**
     * @param text what the user typed; blank text is ignored
     */
    public void send(@NonNull String text) {
        String body = text.trim();
        if (body.isEmpty() || Boolean.TRUE.equals(sending.getValue())) {
            return;
        }
        sending.setValue(true);
        repository.send(body, new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull ChatMessage value) {
                sending.setValue(false);
                error.setValue(null);
                append(List.of(value));
                sent.setValue(new Event<>(true));
            }

            @Override
            public void onError(@NonNull ApiException e) {
                sending.setValue(false);
                error.setValue(ErrorMapper.bannerFor(e));
            }
        });
    }

    private Long lastId() {
        List<ChatMessage> current = messages.getValue();
        return current == null || current.isEmpty() ? null : current.get(current.size() - 1).id();
    }

    private void append(List<ChatMessage> newer) {
        List<ChatMessage> list = new ArrayList<>(messages.getValue() == null ? List.of() : messages.getValue());
        Long last = lastId();
        boolean changed = false;
        for (ChatMessage message : newer) {
            if (last == null || message.id() > last) {
                list.add(message);
                last = message.id();
                changed = true;
            }
        }
        if (changed) {
            messages.setValue(Collections.unmodifiableList(list));
        }
    }

    /** @return messages, oldest first */
    @NonNull
    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    /** @return whether the first load is running */
    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** @return whether a message is being sent */
    @NonNull
    public LiveData<Boolean> getSending() {
        return sending;
    }

    /** @return error banner text, or {@code null} */
    @NonNull
    public LiveData<UiText> getError() {
        return error;
    }

    /** @return fired after a message was sent (clear the input) */
    @NonNull
    public LiveData<Event<Boolean>> getSent() {
        return sent;
    }
}
