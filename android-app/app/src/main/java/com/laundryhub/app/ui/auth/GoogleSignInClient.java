package com.laundryhub.app.ui.auth;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/** "Continue with Google": gets a Google ID token for {@code POST /api/auth/google}. */
// OOP: Abstraction — screens do not know Credential Manager is used.
public interface GoogleSignInClient {

    /** Result of the Google account picker. */
    interface Callback {
        /**
         * @param idToken Google ID token
         */
        void onIdToken(@NonNull String idToken);

        /**
         * @param message why there is no token (cancelled, not configured, failed)
         */
        void onFailure(@StringRes int message);
    }

    /**
     * Shows the Google account picker.
     *
     * @param activity the current screen
     * @param callback result, on the main thread
     */
    void signIn(@NonNull Activity activity, @NonNull Callback callback);
}
