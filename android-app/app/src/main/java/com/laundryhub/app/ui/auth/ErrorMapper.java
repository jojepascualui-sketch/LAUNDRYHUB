package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;

import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.ui.common.UiText;

/** Turns an {@link ApiException} into the text shown in the error banner. */
public final class ErrorMapper {

    private ErrorMapper() {
    }

    /**
     * @param error the failure
     * @return the server's message, a network message, or a generic one
     */
    @NonNull
    public static UiText bannerFor(@NonNull ApiException error) {
        if (error.isNetworkError()) {
            return UiText.of(R.string.error_network);
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? UiText.of(R.string.error_unexpected) : UiText.of(message);
    }
}
