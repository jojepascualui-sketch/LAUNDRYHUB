package com.laundryhub.app.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.function.Supplier;

/**
 * Creates one ViewModel type from a supplier, so ViewModels can take their repositories in the
 * constructor.
 *
 * @param <V> ViewModel type
 */
// OOP: Inheritance — implements ViewModelProvider.Factory.
public final class ViewModelFactory<V extends ViewModel> implements ViewModelProvider.Factory {

    // OOP: Encapsulation — the type and supplier are fixed.
    private final Class<V> type;
    private final Supplier<V> supplier;

    /**
     * @param type     ViewModel class
     * @param supplier creates it
     */
    public ViewModelFactory(@NonNull Class<V> type, @NonNull Supplier<V> supplier) {
        this.type = type;
        this.supplier = supplier;
    }

    // OOP: Polymorphism — called by ViewModelProvider through the Factory interface.
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (!modelClass.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Unknown ViewModel " + modelClass.getName());
        }
        return modelClass.cast(supplier.get());
    }
}
