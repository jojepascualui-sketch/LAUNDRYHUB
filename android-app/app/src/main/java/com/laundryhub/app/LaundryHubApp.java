package com.laundryhub.app;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.session.EncryptedSessionStore;
import com.laundryhub.app.data.session.SessionStore;

/** Application class: owns the {@link AppContainer}. */
// OOP: Inheritance — extends Android's Application.
public class LaundryHubApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this, createSessionStore());
    }

    /** @return the session storage: encrypted on devices; tests override it (no Keystore there) */
    // OOP: Polymorphism — subclasses may supply another SessionStore.
    @NonNull
    protected SessionStore createSessionStore() {
        return new EncryptedSessionStore(this);
    }

    /**
     * @param context any context of this app
     * @return the shared container
     */
    @NonNull
    public static AppContainer container(@NonNull Context context) {
        return ((LaundryHubApp) context.getApplicationContext()).container;
    }
}
