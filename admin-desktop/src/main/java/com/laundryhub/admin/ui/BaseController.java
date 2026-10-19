package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.AdminApi;
import com.laundryhub.admin.api.ApiException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import javafx.application.Platform;

/**
 * Shared behavior of every screen controller: access to the API and navigation, API calls that
 * come back on the JavaFX thread, one place for errors, and sign-out when the session expires.
 */
// OOP: Abstraction — screens extend this class and only describe what they show.
// OOP: Inheritance — LoginController, ShellController and every PageController extend it.
public abstract class BaseController {

    private static final String UNEXPECTED = "Something went wrong. Please try again.";

    // OOP: Encapsulation — the context is set once by the loader.
    private AppContext context;

    /**
     * Called by the loader after FXML injection.
     *
     * @param context shared objects
     */
    public final void attach(AppContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        this.context = context;
        onAttach();
    }

    /** Hook: set up the screen once the context is available. */
    protected void onAttach() {
    }

    /** @return shared objects */
    protected AppContext context() {
        return context;
    }

    /** @return the backend */
    protected AdminApi api() {
        return context.api();
    }

    /** @return navigation */
    protected Navigator navigator() {
        return context.navigator();
    }

    /**
     * Runs an API call and handles the result on the JavaFX thread. Errors go to {@link #showError}.
     *
     * @param call      the running call
     * @param onSuccess receives the value
     * @param <T>       result type
     */
    protected <T> void call(CompletableFuture<T> call, Consumer<T> onSuccess) {
        call(call, onSuccess, this::showError);
    }

    /**
     * @param call      the running call
     * @param onSuccess receives the value
     * @param onError   receives the message for the admin
     * @param <T>       result type
     */
    protected <T> void call(CompletableFuture<T> call, Consumer<T> onSuccess, Consumer<String> onError) {
        context.callStarted();
        call.whenComplete((value, error) -> Platform.runLater(() -> {
            try {
                if (error == null) {
                    onSuccess.accept(value);
                    return;
                }
                ApiException failure = unwrap(error);
                if (failure != null && failure.isUnauthorized() && context.session() != null) {
                    context.signOut();
                    navigator().showSignIn();
                } else {
                    onError.accept(failure == null || failure.getMessage() == null ? UNEXPECTED : failure.getMessage());
                }
            } finally {
                context.callFinished();
            }
        }));
    }

    /**
     * Shows an error message. Pages show a toast; the sign-in screen overrides it with its banner.
     *
     * @param message text for the admin
     */
    // OOP: Polymorphism — LoginController shows errors in its own banner.
    protected void showError(String message) {
        navigator().toast(message);
    }

    private static ApiException unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof ApiException api ? api : null;
    }
}
