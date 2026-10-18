package com.laundryhub.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.laundryhub.app.R;
import com.laundryhub.app.data.auth.SignUpForm;

import org.junit.Test;

import java.util.Map;

public class SignUpValidatorTest {

    private static SignUpForm form(String mobile, String password, String confirm, boolean terms) {
        return new SignUpForm("Juan Dela Cruz", mobile, "juan@example.com", password, confirm, terms);
    }

    @Test
    public void aCompleteFormIsValid() {
        assertTrue(SignUpValidator.validate(form("0917 123 4567", "secret-pass", "secret-pass", true)).isEmpty());
    }

    @Test
    public void everyRuleHasItsMessage() {
        Map<SignUpField, Integer> errors = SignUpValidator.validate(
                new SignUpForm("", "12345", "not-an-email", "short", "different", false));

        assertEquals(Integer.valueOf(R.string.error_required), errors.get(SignUpField.FULL_NAME));
        assertEquals(Integer.valueOf(R.string.error_mobile), errors.get(SignUpField.MOBILE));
        assertEquals(Integer.valueOf(R.string.error_email), errors.get(SignUpField.EMAIL));
        assertEquals(Integer.valueOf(R.string.error_password_length), errors.get(SignUpField.PASSWORD));
        assertEquals(Integer.valueOf(R.string.error_password_match), errors.get(SignUpField.CONFIRM_PASSWORD));
        assertEquals(Integer.valueOf(R.string.error_terms), errors.get(SignUpField.TERMS));
    }

    @Test
    public void serverFieldNamesMapToInputs() {
        assertEquals(SignUpField.CONFIRM_PASSWORD, SignUpField.fromApi("passwordConfirmed"));
        assertEquals(SignUpField.MOBILE, SignUpField.fromApi("mobileNumber"));
        assertEquals(SignUpField.TERMS, SignUpField.fromApi("acceptedTerms"));
    }
}
