package com.laundryhub.app.data.auth;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiCalls;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.AuthApi;
import com.laundryhub.app.data.api.dto.AuthResponse;
import com.laundryhub.app.data.api.dto.GoogleLoginRequest;
import com.laundryhub.app.data.api.dto.LoginRequest;
import com.laundryhub.app.data.api.dto.RegisterRequest;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.data.session.SessionManager;
import com.laundryhub.app.model.Role;

import retrofit2.Call;

/**
 * Calls the backend and starts the session. Admin and staff sessions are not kept on the phone:
 * those accounts use the desktop console.
 */
// OOP: Inheritance — implements the AuthRepository contract.
public class RemoteAuthRepository implements AuthRepository {

    // OOP: Encapsulation — collaborators are fixed at construction.
    private final AuthApi api;
    private final SessionManager sessions;

    /**
     * @param api      auth endpoints
     * @param sessions where the session is kept
     */
    public RemoteAuthRepository(@NonNull AuthApi api, @NonNull SessionManager sessions) {
        this.api = api;
        this.sessions = sessions;
    }

    // OOP: Polymorphism — the Retrofit-backed implementation of each AuthRepository method.
    @Override
    public void signIn(@NonNull String emailOrMobile, @NonNull String password, @NonNull Role tab, boolean remember,
                       @NonNull ApiCallback<Session> callback) {
        run(api.login(new LoginRequest(emailOrMobile, password, tab.name())), remember, callback);
    }

    @Override
    public void register(@NonNull SignUpForm form, @NonNull ApiCallback<Session> callback) {
        run(api.register(new RegisterRequest(form.fullName(), form.mobileNumber(), form.email(), form.password(),
                form.confirmPassword(), form.acceptedTerms())), true, callback);
    }

    @Override
    public void signInWithGoogle(@NonNull String idToken, @NonNull ApiCallback<Session> callback) {
        run(api.google(new GoogleLoginRequest(idToken)), true, callback);
    }

    @Override
    public void signOut() {
        sessions.end();
    }

    private void run(Call<AuthResponse> call, boolean remember, ApiCallback<Session> callback) {
        ApiCalls.enqueue(call, new ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(@NonNull AuthResponse value) {
                Session session;
                try {
                    session = Session.from(value);
                } catch (IllegalArgumentException e) {
                    callback.onError(new ApiException(ApiException.NO_RESPONSE, null, null));
                    return;
                }
                if (session.getRole().usesMobileApp()) {
                    sessions.start(session, remember);
                }
                callback.onSuccess(session);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }
}
