package com.laundryhub.admin.ui;

import com.laundryhub.admin.AppConfig;
import com.laundryhub.admin.api.AdminApi;
import com.laundryhub.admin.model.Session;

import java.util.ArrayList;
import java.util.List;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

/** Shared objects for every controller: settings, API, session, navigation and browser links. */
public final class AppContext {

    // OOP: Encapsulation — the session changes only through signIn / signOut.
    private final AppConfig config;
    private final AdminApi api;
    private final Navigator navigator;
    private final HostServices hostServices;
    private Session session;
    private int pendingCalls;
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper(false);
    private final List<Runnable> idleListeners = new ArrayList<>();

    /**
     * @param config       settings
     * @param api          backend
     * @param navigator    screen switching
     * @param hostServices opens links in the browser (may be {@code null} in tests)
     */
    public AppContext(AppConfig config, AdminApi api, Navigator navigator, HostServices hostServices) {
        if (config == null || api == null || navigator == null) {
            throw new IllegalArgumentException("config, api and navigator are required");
        }
        this.config = config;
        this.api = api;
        this.navigator = navigator;
        this.hostServices = hostServices;
    }

    /** @return settings */
    public AppConfig config() {
        return config;
    }

    /** @return backend */
    public AdminApi api() {
        return api;
    }

    /** @return navigation */
    public Navigator navigator() {
        return navigator;
    }

    /** @return the signed-in user, or {@code null} */
    public Session session() {
        return session;
    }

    /**
     * @param session the new session
     */
    public void signIn(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }
        this.session = session;
    }

    /** Forgets the token (kept in memory only). */
    public void signOut() {
        session = null;
    }

    /** @return the bearer token, or {@code null} */
    public String token() {
        return session == null ? null : session.token();
    }

    /**
     * @param url web page to open in the default browser
     */
    public void openInBrowser(String url) {
        if (hostServices != null && url != null && !url.isBlank()) {
            hostServices.showDocument(url);
        }
    }

    /** Counts a running API call (the screenshot tour waits until none are left). */
    void callStarted() {
        pendingCalls++;
        busy.set(true);
    }

    /** @return true while an API call is running (the header shows the loading spinner) */
    public ReadOnlyBooleanProperty busyProperty() {
        return busy.getReadOnlyProperty();
    }

    /** Marks an API call as finished. */
    void callFinished() {
        pendingCalls = Math.max(0, pendingCalls - 1);
        if (pendingCalls == 0) {
            busy.set(false);
            List<Runnable> listeners = new ArrayList<>(idleListeners);
            idleListeners.clear();
            listeners.forEach(Runnable::run);
        }
    }

    /**
     * @param listener runs on the FX thread once no API call is running
     */
    public void whenIdle(Runnable listener) {
        if (pendingCalls == 0) {
            listener.run();
        } else {
            idleListeners.add(listener);
        }
    }
}
