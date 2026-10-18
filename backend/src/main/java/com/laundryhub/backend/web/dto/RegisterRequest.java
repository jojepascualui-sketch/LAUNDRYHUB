package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 00b "Create account".
 *
 * @param fullName        Full name
 * @param mobileNumber    Mobile number, 09XXXXXXXXX
 * @param email           Email
 * @param password        Password, at least 8 characters
 * @param confirmPassword Confirm password, must equal {@code password}
 * @param acceptedTerms   the Terms checkbox (mentions location use); must be ticked
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = ApiValidation.MOBILE_REGEX, message = ApiValidation.MOBILE_MESSAGE) String mobileNumber,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = ApiValidation.MIN_PASSWORD_LENGTH, max = ApiValidation.MAX_PASSWORD_LENGTH) String password,
        @NotBlank String confirmPassword,
        @AssertTrue(message = "must be accepted") boolean acceptedTerms) {

    /** @return whether both password fields match */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
