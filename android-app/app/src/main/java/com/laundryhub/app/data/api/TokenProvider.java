package com.laundryhub.app.data.api;

import androidx.annotation.Nullable;

/** Supplies the bearer token to {@link AuthInterceptor}. */
// OOP: Abstraction — the interceptor does not know where or how the token is stored.
public interface TokenProvider {

    /** @return the current token, or {@code null} when signed out */
    @Nullable
    String getToken();

    /** Called when the server rejects the token (401): the session is over. */
    void onUnauthorized();
}
