package com.laundryhub.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.auth.SignUpForm;
import com.laundryhub.app.ui.common.UiText;

import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

public class SignUpViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void validFormIsSentWithANormalizedMobile() {
        FakeAuthRepository repo = new FakeAuthRepository();
        SignUpViewModel vm = new SignUpViewModel(repo);

        vm.createAccount(new SignUpForm("Juan Dela Cruz", "0917 123 4567", "juan@example.com", "secret-pass",
                "secret-pass", true));

        assertTrue(vm.getForm().getValue().loading());
        assertEquals("09171234567", repo.lastSignUp.mobileNumber());
    }

    @Test
    public void serverFieldErrorsLandOnTheirInputs() {
        FakeAuthRepository repo = new FakeAuthRepository();
        SignUpViewModel vm = new SignUpViewModel(repo);
        vm.createAccount(new SignUpForm("Juan", "09171234567", "juan@example.com", "secret-pass", "secret-pass", true));

        repo.pending.onError(new ApiException(400, "Some fields are not valid.",
                Map.of("passwordConfirmed", "Passwords do not match")));

        assertEquals(UiText.of("Passwords do not match"),
                vm.getForm().getValue().errorFor(SignUpField.CONFIRM_PASSWORD));
    }

    @Test
    public void duplicateEmailShowsTheServerMessage() {
        FakeAuthRepository repo = new FakeAuthRepository();
        SignUpViewModel vm = new SignUpViewModel(repo);
        vm.createAccount(new SignUpForm("Juan", "09171234567", "juan@example.com", "secret-pass", "secret-pass", true));

        repo.pending.onError(new ApiException(409, "An account with this email already exists. Sign in instead.", null));

        assertEquals(UiText.of("An account with this email already exists. Sign in instead."),
                vm.getForm().getValue().banner());
    }
}
