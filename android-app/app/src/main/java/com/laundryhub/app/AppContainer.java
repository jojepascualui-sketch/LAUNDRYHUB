package com.laundryhub.app;

import android.content.Context;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.api.ApiClient;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.auth.RemoteAuthRepository;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.data.order.RemoteOrderRepository;
import com.laundryhub.app.data.feedback.FeedbackRepository;
import com.laundryhub.app.data.feedback.RemoteFeedbackRepository;
import com.laundryhub.app.data.rider.RemoteRiderRepository;
import com.laundryhub.app.data.rider.RiderRepository;
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
    private final RiderRepository riderRepository;
    private final FeedbackRepository feedbackRepository;

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
        riderRepository = new RemoteRiderRepository(api.rider());
        feedbackRepository = new RemoteFeedbackRepository(api.feedback());
    }

    /**
     * Explicit parts (tests and screenshot renders use fakes).
     *
     * @param sessions        session holder
     * @param authRepository  sign-in
     * @param googleSignIn    Google sign-in
     * @param orderRepository customer orders
     * @param riderRepository    rider bookings and jobs
     * @param feedbackRepository feedback chat and ratings
     */
    public AppContainer(@NonNull SessionManager sessions, @NonNull AuthRepository authRepository,
                        @NonNull GoogleSignInClient googleSignIn, @NonNull OrderRepository orderRepository,
                        @NonNull RiderRepository riderRepository,
                        @NonNull FeedbackRepository feedbackRepository) {
        this.sessions = sessions;
        this.authRepository = authRepository;
        this.googleSignIn = googleSignIn;
        this.orderRepository = orderRepository;
        this.riderRepository = riderRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** @return feedback chat and order ratings */
    @NonNull
    public FeedbackRepository feedbackRepository() {
        return feedbackRepository;
    }

    /** @return rider bookings, jobs and GPS (R1–R5) */
    @NonNull
    public RiderRepository riderRepository() {
        return riderRepository;
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
