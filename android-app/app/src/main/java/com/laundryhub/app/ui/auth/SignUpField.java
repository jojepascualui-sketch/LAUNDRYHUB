package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** The 00b inputs, with the field names the server uses in its {@code errors} map. */
public enum SignUpField {
    /** Full name. */
    FULL_NAME("fullName"),
    /** Mobile number. */
    MOBILE("mobileNumber"),
    /** Email. */
    EMAIL("email"),
    /** Password. */
    PASSWORD("password"),
    /** Confirm password ("passwordConfirmed" is the server's match check). */
    CONFIRM_PASSWORD("confirmPassword", "passwordConfirmed"),
    /** Terms checkbox. */
    TERMS("acceptedTerms");

    // OOP: Encapsulation — server names are fixed per field.
    private final String[] apiNames;

    SignUpField(String... apiNames) {
        this.apiNames = apiNames;
    }

    /**
     * @param apiName field name from a server error
     * @return the matching field, or {@code null}
     */
    @Nullable
    public static SignUpField fromApi(@NonNull String apiName) {
        for (SignUpField field : values()) {
            for (String name : field.apiNames) {
                if (name.equals(apiName)) {
                    return field;
                }
            }
        }
        return null;
    }
}
