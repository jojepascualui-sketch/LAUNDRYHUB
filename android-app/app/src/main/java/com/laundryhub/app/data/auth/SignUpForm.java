package com.laundryhub.app.data.auth;

import androidx.annotation.NonNull;

/**
 * The 00b fields, already validated by {@code SignUpValidator}.
 *
 * @param fullName        Full name
 * @param mobileNumber    09XXXXXXXXX without spaces
 * @param email           Email
 * @param password        Password
 * @param confirmPassword Confirm password
 * @param acceptedTerms   terms checkbox
 */
public record SignUpForm(@NonNull String fullName, @NonNull String mobileNumber, @NonNull String email,
                         @NonNull String password, @NonNull String confirmPassword, boolean acceptedTerms) {
}
