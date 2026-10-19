package com.laundryhub.app.ui.common;

import androidx.annotation.NonNull;

/** Runs only the last of several quick requests (e.g. the live quote while tapping + fast). */
// OOP: Abstraction — the ViewModel does not know whether a Handler or a test runs the task.
public interface Debouncer {

    /**
     * Replaces any pending task with this one.
     *
     * @param task what to run after the quiet period
     */
    void submit(@NonNull Runnable task);

    /** Drops the pending task. */
    void cancel();
}
