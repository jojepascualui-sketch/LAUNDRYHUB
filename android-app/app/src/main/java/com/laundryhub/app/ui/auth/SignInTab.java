package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;
import com.laundryhub.app.model.Role;

/**
 * The Customer / Rider / Admin segments on 00a. Each tab changes the heading, subheading,
 * email label and placeholder, whether "Continue with Google" shows, and which role the server
 * expects — exactly as in docs/design/source/Auth.dc.html. Order = segment order.
 */
public enum SignInTab {

    /** "Welcome back" · Email · Google shown. */
    CUSTOMER(R.string.sign_in_heading_customer, R.string.sign_in_sub_customer, R.string.label_email,
            R.string.hint_email, Role.CUSTOMER) {
        @Override
        public boolean showsGoogle() {
            return true;
        }
    },

    /** "Rider sign in" · "Rider email or mobile" · no Google. */
    RIDER(R.string.sign_in_heading_rider, R.string.sign_in_sub_rider, R.string.label_rider_email,
            R.string.hint_email, Role.RIDER) {
        @Override
        public boolean showsGoogle() {
            return false;
        }

        @Nullable
        @Override
        public Integer validateIdentifier(@NonNull String identifier) {
            if (identifier.isEmpty()) {
                return R.string.error_required;
            }
            return AuthValidator.isEmail(identifier) || AuthValidator.isMobile(identifier)
                    ? null : R.string.error_email_or_mobile;
        }

        @NonNull
        @Override
        public String normalizeIdentifier(@NonNull String identifier) {
            return AuthValidator.isMobile(identifier) ? AuthValidator.normalizeMobile(identifier) : identifier;
        }
    },

    /** "Admin sign in" · "Staff email" · no Google; signs in to the desktop console. */
    ADMIN(R.string.sign_in_heading_admin, R.string.sign_in_sub_admin, R.string.label_staff_email,
            R.string.hint_staff_email, Role.ADMIN) {
        @Override
        public boolean showsGoogle() {
            return false;
        }
    };

    // OOP: Encapsulation — each tab's texts are fixed at declaration.
    @StringRes
    private final int heading;
    @StringRes
    private final int subheading;
    @StringRes
    private final int emailLabel;
    @StringRes
    private final int emailHint;
    private final Role role;

    SignInTab(@StringRes int heading, @StringRes int subheading, @StringRes int emailLabel,
              @StringRes int emailHint, Role role) {
        this.heading = heading;
        this.subheading = subheading;
        this.emailLabel = emailLabel;
        this.emailHint = emailHint;
        this.role = role;
    }

    /** @return whether "or" and "Continue with Google" are shown (customers only) */
    // OOP: Polymorphism — each tab answers for itself.
    public abstract boolean showsGoogle();

    /**
     * @param identifier trimmed email field
     * @return an error message resource, or {@code null} if valid
     */
    // OOP: Polymorphism — the Rider tab also accepts a mobile number.
    @Nullable
    public Integer validateIdentifier(@NonNull String identifier) {
        if (identifier.isEmpty()) {
            return R.string.error_required;
        }
        return AuthValidator.isEmail(identifier) ? null : R.string.error_email;
    }

    /**
     * @param identifier valid email field
     * @return the value to send to the server
     */
    @NonNull
    public String normalizeIdentifier(@NonNull String identifier) {
        return identifier;
    }

    /** @return heading, e.g. "Welcome back" */
    @StringRes
    public int getHeading() {
        return heading;
    }

    /** @return subheading */
    @StringRes
    public int getSubheading() {
        return subheading;
    }

    /** @return email field label */
    @StringRes
    public int getEmailLabel() {
        return emailLabel;
    }

    /** @return email field placeholder */
    @StringRes
    public int getEmailHint() {
        return emailHint;
    }

    /** @return role sent to the server for this tab */
    @NonNull
    public Role getRole() {
        return role;
    }

    /**
     * @param index segment index
     * @return the tab
     */
    @NonNull
    public static SignInTab atIndex(int index) {
        return values()[index];
    }
}
