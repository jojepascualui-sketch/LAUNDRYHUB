package com.laundryhub.app.data.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.laundryhub.app.data.api.dto.AuthResponse;
import com.laundryhub.app.data.api.dto.UserProfile;
import com.laundryhub.app.model.Role;

import org.junit.Test;

public class SessionManagerTest {

    /** In-memory store for tests. */
    private static final class MemoryStore implements SessionStore {
        Session saved;

        @Override
        public void save(Session session) {
            saved = session;
        }

        @Override
        public Session load() {
            return saved;
        }

        @Override
        public void clear() {
            saved = null;
        }
    }

    private long now = 1_000L;

    @Test
    public void rememberMeDecidesWhetherTheSessionIsSaved() {
        MemoryStore store = new MemoryStore();
        SessionManager sessions = new SessionManager(store, () -> now);
        Session session = new Session("jwt", 5_000L, Role.CUSTOMER, "Ana");

        sessions.start(session, false);
        assertEquals("jwt", sessions.getToken());
        assertNull(store.saved);

        sessions.start(session, true);
        assertEquals(session, store.saved);
        assertEquals(session, new SessionManager(store, () -> now).getActive());
    }

    @Test
    public void expiredOrRejectedTokensEndTheSession() {
        MemoryStore store = new MemoryStore();
        SessionManager sessions = new SessionManager(store, () -> now);
        sessions.start(new Session("jwt", 5_000L, Role.RIDER, "Rider"), true);

        now = 5_000L;
        assertNull(sessions.getActive());
        assertNull(store.saved);

        now = 1_000L;
        sessions.start(new Session("jwt", 5_000L, Role.RIDER, "Rider"), true);
        sessions.onUnauthorized();
        assertNull(sessions.getToken());
    }

    @Test
    public void sessionIsReadFromTheSignInResponse() {
        Session session = Session.from(new AuthResponse("jwt", "2026-10-19T02:00:00Z",
                new UserProfile(7L, "STAFF", "Counter Staff")));

        assertEquals(Role.STAFF, session.getRole());
        assertEquals(1_792_375_200_000L, session.getExpiresAtMillis());
        assertEquals(false, session.getRole().usesMobileApp());
    }
}
