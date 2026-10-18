package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;

import com.laundryhub.app.R;
import com.laundryhub.app.data.auth.SignUpForm;

import java.util.EnumMap;
import java.util.Map;

/** Checks the 00b form before it is sent. */
public final class SignUpValidator {

    private SignUpValidator() {
    }

    /**
     * @param form the entered values (mobile may still contain spaces)
     * @return field → error message resource; empty when valid
     */
    @NonNull
    public static Map<SignUpField, Integer> validate(@NonNull SignUpForm form) {
        Map<SignUpField, Integer> errors = new EnumMap<>(SignUpField.class);
        if (form.fullName().isEmpty()) {
            errors.put(SignUpField.FULL_NAME, R.string.error_required);
        }
        if (form.mobileNumber().isEmpty()) {
            errors.put(SignUpField.MOBILE, R.string.error_required);
        } else if (!AuthValidator.isMobile(form.mobileNumber())) {
            errors.put(SignUpField.MOBILE, R.string.error_mobile);
        }
        if (form.email().isEmpty()) {
            errors.put(SignUpField.EMAIL, R.string.error_required);
        } else if (!AuthValidator.isEmail(form.email())) {
            errors.put(SignUpField.EMAIL, R.string.error_email);
        }
        if (!AuthValidator.isLongEnough(form.password())) {
            errors.put(SignUpField.PASSWORD, R.string.error_password_length);
        }
        if (form.confirmPassword().isEmpty()) {
            errors.put(SignUpField.CONFIRM_PASSWORD, R.string.error_required);
        } else if (!form.confirmPassword().equals(form.password())) {
            errors.put(SignUpField.CONFIRM_PASSWORD, R.string.error_password_match);
        }
        if (!form.acceptedTerms()) {
            errors.put(SignUpField.TERMS, R.string.error_terms);
        }
        return errors;
    }
}
