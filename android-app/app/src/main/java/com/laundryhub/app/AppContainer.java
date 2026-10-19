package com.laundryhub.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.api.ApiClient;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.auth.RemoteAuthRepository;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.data.order.RemoteOrderRepository;
import com.laundryhub.app.data.session.SessionStore;
import com.laundryhub.app.data.session.SessionManager;
import com.laundryhub.app.ui.auth.CredentialManagerGoogleSignIn;
import com.laundryhub.app.ui.auth.GoogleSignInClient;

/** Creates the app's shared objects once (manual dependency injection). */
public final class AppContainer {

    // OOP: Encapsulation — singletons are created here and exposed read-only.
    private final SessionManager sessions;
    private final AuthRepository authRepository;
    private final GoogleSignInClient googleSignIn;
    private final OrderRepository orderRepository;

    /**
     * The real app: everything talks to {@code BuildConfig.API_BASE_URL}.
     *
     * @param context      application context
     * @param sessionStore where a remembered session is kept
     */
    public AppContainer(@NonNull Context context, @NonNull SessionStore sessionStore) {
        sessions = new SessionManager(sessionStore, System::currentTimeMillis);
        ApiClient api = new ApiClient(BuildConfig.API_BASE_URL, sessions);
        authRepository = new RemoteAuthRepository(api.auth(), sessions);
        googleSignIn = new CredentialManagerGoogleSignIn(BuildConfig.GOOGLE_WEB_CLIENT_ID);
        orderRepository = new RemoteOrderRepository(api.catalog(), api.orders());
    }

    /**
     * Explicit parts (tests and screenshot renders use fakes).
     *
     * @param sessions        session holder
     * @param authRepository  sign-in
     * @param googleSignIn    Google sign-in
     * @param orderRepository customer orders
     */
    public AppContainer(@NonNull SessionManager sessions, @NonNull AuthRepository authRepository,
                        @NonNull GoogleSignInClient googleSignIn, @NonNull OrderRepository orderRepository) {
        this.sessions = sessions;
        this.authRepository = authRepository;
        this.googleSignIn = googleSignIn;
        this.orderRepository = orderRepository;
    }

    /** @return customer order flow (01–06) */
    @NonNull
    public OrderRepository orderRepository() {
        return orderRepository;
    }

    /** @return the session holder */
    @NonNull
    public SessionManager sessions() {
        return sessions;
    }

    /** @return sign-in and sign-up */
    @NonNull
    public AuthRepository authRepository() {
        return authRepository;
    }

    /** @return "Continue with Google" */
    @NonNull
    public GoogleSignInClient googleSignIn() {
        return googleSignIn;
    }
}
