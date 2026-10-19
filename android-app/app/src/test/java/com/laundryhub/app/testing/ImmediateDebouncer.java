package com.laundryhub.app.testing;

import androidx.annotation.NonNull;

import com.laundryhub.app.ui.common.Debouncer;

/** Runs the task at once (unit tests). */
public class ImmediateDebouncer implements Debouncer {

    @Override
    public void submit(@NonNull Runnable task) {
        task.run();
    }

    @Override
    public void cancel() {
    }
}
