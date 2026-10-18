package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.ui.common.UiText;

/**
 * What the sign-in form shows: busy state and errors (no design for errors; same style, warn tokens).
 *
 * @param loading         a request is running ("Signing in…", inputs disabled)
 * @param identifierError error under the email field, or {@code null}
 * @param passwordError   error under the password field, or {@code null}
 * @param banner          error above the Sign in button, or {@code null}
 */
public record SignInFormState(boolean loading, @Nullable UiText identifierError, @Nullable UiText passwordError,
                              @Nullable UiText banner) {

    /** @return the empty, idle form */
    @NonNull
    public static SignInFormState idle() {
        return new SignInFormState(false, null, null, null);
    }

    /** @return the form while a request runs */
    @NonNull
    public static SignInFormState busy() {
        return new SignInFormState(true, null, null, null);
    }
}
