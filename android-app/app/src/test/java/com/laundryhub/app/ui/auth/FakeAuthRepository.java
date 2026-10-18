package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.auth.SignUpForm;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.model.Role;

/** Records calls; the test completes them with {@link #pending}. */
class FakeAuthRepository implements AuthRepository {

    String lastIdentifier;
    Role lastTab;
    boolean lastRemember;
    SignUpForm lastSignUp;
    int calls;
    ApiCallback<Session> pending;

    @Override
    public void signIn(@NonNull String emailOrMobile, @NonNull String password, @NonNull Role tab, boolean remember,
                       @NonNull ApiCallback<Session> callback) {
        calls++;
        lastIdentifier = emailOrMobile;
        lastTab = tab;
        lastRemember = remember;
        pending = callback;
    }

    @Override
    public void register(@NonNull SignUpForm form, @NonNull ApiCallback<Session> callback) {
        calls++;
        lastSignUp = form;
        pending = callback;
    }

    @Override
    public void signInWithGoogle(@NonNull String idToken, @NonNull ApiCallback<Session> callback) {
        calls++;
        pending = callback;
    }

    @Override
    public void signOut() {
    }
}
