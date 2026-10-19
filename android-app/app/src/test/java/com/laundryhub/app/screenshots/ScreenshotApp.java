package com.laundryhub.app.screenshots;

import androidx.annotation.NonNull;

import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.auth.SignUpForm;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.data.session.SessionManager;
import com.laundryhub.app.data.session.SessionStore;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.testing.FakeFeedbackRepository;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.testing.FakeRiderRepository;

/** The app with a signed-in user and the fake backends (no network, no Keystore). */
public class ScreenshotApp extends LaundryHubApp {

    @NonNull
    @Override
    protected AppContainer createContainer() {
        SessionManager sessions = new SessionManager(new SessionStore() {
            @Override
            public void save(@NonNull Session session) {
            }

            @Override
            public Session load() {
                return null;
            }

            @Override
            public void clear() {
            }
        }, System::currentTimeMillis);
        sessions.start(new Session("test-token", Long.MAX_VALUE, Role.CUSTOMER, "Ana Reyes"), false);
        AuthRepository noAuth = new AuthRepository() {
            @Override
            public void signIn(@NonNull String id, @NonNull String pw, @NonNull Role tab, boolean remember,
                               @NonNull ApiCallback<Session> callback) {
            }

            @Override
            public void register(@NonNull SignUpForm form, @NonNull ApiCallback<Session> callback) {
            }

            @Override
            public void signInWithGoogle(@NonNull String idToken, @NonNull ApiCallback<Session> callback) {
            }

            @Override
            public void signOut() {
            }
        };
        return new AppContainer(sessions, noAuth, (activity, callback) -> { }, new FakeOrderRepository(),
                new FakeRiderRepository(), new FakeFeedbackRepository());
    }
}
