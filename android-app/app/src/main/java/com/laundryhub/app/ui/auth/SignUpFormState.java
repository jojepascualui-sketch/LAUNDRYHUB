package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.ui.common.UiText;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * What the 00b form shows.
 *
 * @param loading     a request is running ("Creating account…")
 * @param fieldErrors field → error line
 * @param banner      error above "Create account", or {@code null}
 */
public record SignUpFormState(boolean loading, @NonNull Map<SignUpField, UiText> fieldErrors,
                              @Nullable UiText banner) {

    /** Copies the map so the state cannot change later. */
    public SignUpFormState {
        fieldErrors = fieldErrors.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(fieldErrors));
    }

    /** @return the idle form */
    @NonNull
    public static SignUpFormState idle() {
        return new SignUpFormState(false, Collections.emptyMap(), null);
    }

    /**
     * @param field an input
     * @return its error, or {@code null}
     */
    @Nullable
    public UiText errorFor(@NonNull SignUpField field) {
        return fieldErrors.get(field);
    }
}
