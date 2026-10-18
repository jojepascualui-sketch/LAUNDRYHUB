package com.laundryhub.app.data.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Persists the session between app launches. */
// OOP: Abstraction — SessionManager does not know whether storage is encrypted, in memory or a fake.
public interface SessionStore {

    /**
     * @param session the session to keep
     */
    void save(@NonNull Session session);

    /** @return the saved session, or {@code null} */
    @Nullable
    Session load();

    /** Forgets the saved session. */
    void clear();
}
