package com.laundryhub.app.data.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.data.api.TokenProvider;

/**
 * The current session. With "Remember me" it is also saved in the {@link SessionStore};
 * without it, it lives in memory only and is gone when the app process ends.
 */
// OOP: Inheritance — implements TokenProvider so the JWT interceptor can read the token.
public class SessionManager implements TokenProvider {

    /** Supplies the current time (replaceable in tests). */
    public interface Clock {
        /** @return epoch milliseconds */
        long now();
    }

    // OOP: Encapsulation — the session changes only through start() and end().
    private final SessionStore store;
    private final Clock clock;
    @Nullable
    private Session current;

    /**
     * @param store persistent storage
     * @param clock time source
     */
    public SessionManager(@NonNull SessionStore store, @NonNull Clock clock) {
        this.store = store;
        this.clock = clock;
        this.current = store.load();
    }

    /**
     * Starts a session after sign-in.
     *
     * @param session  the session
     * @param remember keep it after the app closes
     */
    public synchronized void start(@NonNull Session session, boolean remember) {
        current = session;
        if (remember) {
            store.save(session);
        } else {
            store.clear();
        }
    }

    /** Signs out: forgets the session everywhere. */
    public synchronized void end() {
        current = null;
        store.clear();
    }

    /** @return the session if signed in and not expired, else {@code null} */
    @Nullable
    public synchronized Session getActive() {
        if (current != null && current.isExpired(clock.now())) {
            end();
        }
        return current;
    }

    // OOP: Polymorphism — TokenProvider methods called by AuthInterceptor.
    @Nullable
    @Override
    public String getToken() {
        Session session = getActive();
        return session == null ? null : session.getToken();
    }

    @Override
    public void onUnauthorized() {
        end();
    }
}
