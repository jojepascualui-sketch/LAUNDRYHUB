package com.laundryhub.app.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ViewAuthHeaderBinding;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.ui.HomeNavigator;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;

/**
 * Shared behavior of the sign-in and sign-up screens: blue header under a transparent status
 * bar, the error banner, "Continue with Google", and routing by role after sign-in.
 */
// OOP: Abstraction — subclasses supply their own form; the common screen behavior lives here.
// OOP: Inheritance — SignInActivity and SignUpActivity extend this class.
public abstract class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForBlueHeader(this);
        super.onCreate(savedInstanceState);
    }

    /**
     * Applies the design's header padding below the status bar and keeps the form above the
     * navigation bar and keyboard.
     *
     * @param header the included header
     * @param form   the scrolling form
     */
    protected void applyInsets(@NonNull ViewAuthHeaderBinding header, @NonNull View form) {
        SystemBars.applyAuthInsets(header.authHeaderContent,
                getResources().getDimensionPixelSize(R.dimen.auth_header_padding_top),
                getResources().getDimensionPixelSize(R.dimen.auth_header_gap), form);
    }

    /**
     * @param header     the included header
     * @param heading    e.g. "Welcome back"
     * @param subheading e.g. "Sign in to book your next laundry."
     */
    protected void setHeader(@NonNull ViewAuthHeaderBinding header, @StringRes int heading,
                             @StringRes int subheading) {
        header.authHeading.setText(heading);
        header.authSubheading.setText(subheading);
    }

    /**
     * @param banner the error banner
     * @param text   message, or {@code null} to hide
     */
    protected void showBanner(@NonNull TextView banner, @Nullable UiText text) {
        banner.setText(text == null ? null : text.resolve(this));
        banner.setVisibility(text == null ? View.GONE : View.VISIBLE);
    }

    /**
     * @param text a resolvable text, may be {@code null}
     * @return the string, or {@code null}
     */
    @Nullable
    protected String resolve(@Nullable UiText text) {
        return text == null ? null : text.resolve(this);
    }

    /**
     * A short message at the bottom of the screen (ink background, design token).
     *
     * @param message string resource
     */
    protected void showMessage(@StringRes int message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ink))
                .setTextColor(ContextCompat.getColor(this, R.color.surface))
                .show();
    }

    /**
     * Opens the Google account picker.
     *
     * @param callback receives the ID token or the failure
     */
    protected void startGoogleSignIn(@NonNull GoogleSignInClient.Callback callback) {
        LaundryHubApp.container(this).googleSignIn().signIn(this, callback);
    }

    /**
     * Routes once after sign-in: Home, Rider home, or the admin-console message.
     *
     * @param event the signed-in event
     */
    protected void onSignedIn(@Nullable Event<Role> event) {
        Role role = event == null ? null : event.take();
        if (role != null) {
            HomeNavigator.open(this, role);
        }
    }
}
