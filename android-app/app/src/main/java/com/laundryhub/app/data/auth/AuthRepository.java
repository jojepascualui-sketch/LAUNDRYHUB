package com.laundryhub.app.data.auth;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.model.Role;

/** Sign-in and sign-up (00a–00c). */
// OOP: Abstraction — ViewModels depend on this interface; tests use a fake.
public interface AuthRepository {

    /**
     * {@code POST /api/auth/login}.
     *
     * @param emailOrMobile e-mail, or a rider's mobile number
     * @param password      password
     * @param tab           selected tab (CUSTOMER, RIDER or ADMIN)
     * @param remember      "Remember me": keep the session after the app closes
     * @param callback      the started session
     */
    void signIn(@NonNull String emailOrMobile, @NonNull String password, @NonNull Role tab, boolean remember,
                @NonNull ApiCallback<Session> callback);

    /**
     * {@code POST /api/auth/register}; the new customer is signed in and remembered.
     *
     * @param form     sign-up fields
     * @param callback the started session
     */
    void register(@NonNull SignUpForm form, @NonNull ApiCallback<Session> callback);

    /**
     * {@code POST /api/auth/google}; the customer is remembered.
     *
     * @param idToken  Google ID token
     * @param callback the started session
     */
    void signInWithGoogle(@NonNull String idToken, @NonNull ApiCallback<Session> callback);

    /** Ends the session and forgets the token. */
    void signOut();
}
