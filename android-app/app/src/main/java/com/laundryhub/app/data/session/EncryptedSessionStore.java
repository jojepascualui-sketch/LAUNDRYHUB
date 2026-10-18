package com.laundryhub.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.laundryhub.app.model.Role;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Keeps the session in EncryptedSharedPreferences (AES-256, key in the Android Keystore). */
// OOP: Inheritance — implements the SessionStore contract.
public class EncryptedSessionStore implements SessionStore {

    /** Preferences file name. */
    public static final String FILE_NAME = "laundryhub_session";

    private static final String KEY_TOKEN = "token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "full_name";

    // OOP: Encapsulation — the encrypted preferences never leave this class.
    private final SharedPreferences prefs;

    /**
     * @param context any context
     * @throws IllegalStateException if the Keystore cannot create the key
     */
    public EncryptedSessionStore(@NonNull Context context) {
        try {
            String keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(FILE_NAME, keyAlias, context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot open encrypted session storage", e);
        }
    }

    // OOP: Polymorphism — the encrypted implementation of SessionStore.
    @Override
    public void save(@NonNull Session session) {
        prefs.edit()
                .putString(KEY_TOKEN, session.getToken())
                .putLong(KEY_EXPIRES_AT, session.getExpiresAtMillis())
                .putString(KEY_ROLE, session.getRole().name())
                .putString(KEY_NAME, session.getFullName())
                .apply();
    }

    @Nullable
    @Override
    public Session load() {
        String token = prefs.getString(KEY_TOKEN, null);
        String role = prefs.getString(KEY_ROLE, null);
        if (token == null || role == null) {
            return null;
        }
        try {
            return new Session(token, prefs.getLong(KEY_EXPIRES_AT, 0L), Role.fromApi(role),
                    prefs.getString(KEY_NAME, ""));
        } catch (IllegalArgumentException e) {
            clear();
            return null;
        }
    }

    @Override
    public void clear() {
        prefs.edit().clear().apply();
    }
}
