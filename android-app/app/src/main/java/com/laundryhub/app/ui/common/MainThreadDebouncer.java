package com.laundryhub.app.ui.common;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/** Debouncer on the main thread with a fixed quiet period. */
// OOP: Inheritance — implements the Debouncer contract.
public class MainThreadDebouncer implements Debouncer {

    // OOP: Encapsulation — the handler and delay are fixed; only one task is pending.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long delayMillis;
    private Runnable pending;

    /**
     * @param delayMillis quiet period, 0 or more
     * @throws IllegalArgumentException if the delay is negative
     */
    public MainThreadDebouncer(long delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Delay must not be negative");
        }
        this.delayMillis = delayMillis;
    }

    // OOP: Polymorphism — the Handler-based implementation of Debouncer.
    @Override
    public void submit(@NonNull Runnable task) {
        cancel();
        pending = task;
        handler.postDelayed(task, delayMillis);
    }

    @Override
    public void cancel() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }
}
