package com.laundryhub.app.data;

import androidx.annotation.NonNull;

/**
 * Result of an asynchronous repository call, delivered on the main thread.
 *
 * @param <T> success value
 */
// OOP: Abstraction — ViewModels get a result without knowing about Retrofit.
public interface ApiCallback<T> {

    /**
     * @param value the result
     */
    void onSuccess(@NonNull T value);

    /**
     * @param error what went wrong
     */
    void onError(@NonNull ApiException error);
}
