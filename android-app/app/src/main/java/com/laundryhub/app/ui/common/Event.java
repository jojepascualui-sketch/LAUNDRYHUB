package com.laundryhub.app.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A one-time LiveData value (navigation, messages): handled once even if the screen
 * re-subscribes after rotation.
 *
 * @param <T> payload type
 */
public final class Event<T> {

    // OOP: Encapsulation — the payload can be taken only once.
    private final T content;
    private boolean handled;

    /**
     * @param content the payload
     */
    public Event(@NonNull T content) {
        this.content = content;
    }

    /** @return the payload the first time, then {@code null} */
    @Nullable
    public synchronized T take() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /** @return the payload, even if already handled (tests) */
    @NonNull
    public T peek() {
        return content;
    }
}
