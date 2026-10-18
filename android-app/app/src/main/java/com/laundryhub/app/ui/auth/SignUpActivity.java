package com.laundryhub.app.ui.auth;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.data.auth.SignUpForm;
import com.laundryhub.app.databinding.ActivitySignUpBinding;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;
import com.laundryhub.app.ui.widget.FormField;

import java.util.EnumMap;
import java.util.Map;

/** 00b-sign-up "Create your account" (customers). */
// OOP: Inheritance — an AuthActivity with the sign-up form.
public class SignUpActivity extends AuthActivity {

    private ActivitySignUpBinding binding;
    private SignUpViewModel viewModel;
    private final Map<SignUpField, FormField> fields = new EnumMap<>(SignUpField.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppContainer app = LaundryHubApp.container(this);
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(SignUpViewModel.class,
                () -> new SignUpViewModel(app.authRepository()))).get(SignUpViewModel.class);
        applyInsets(binding.header, binding.form);
        setHeader(binding.header, R.string.sign_up_heading, R.string.sign_up_sub);

        fields.put(SignUpField.FULL_NAME, binding.fullName);
        fields.put(SignUpField.MOBILE, binding.mobile);
        fields.put(SignUpField.EMAIL, binding.email);
        fields.put(SignUpField.PASSWORD, binding.password);
        fields.put(SignUpField.CONFIRM_PASSWORD, binding.confirmPassword);

        binding.createAccount.setOnClickListener(v -> submit());
        binding.confirmPassword.getInput().setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.google.setOnClickListener(v -> startGoogleSignIn(new GoogleSignInClient.Callback() {
            @Override
            public void onIdToken(@NonNull String idToken) {
                viewModel.signInWithGoogle(idToken);
            }

            @Override
            public void onFailure(int message) {
                viewModel.googleFailed(message);
            }
        }));
        // Back to 00a, which is still underneath.
        binding.goToSignIn.setOnClickListener(v -> finish());

        viewModel.getForm().observe(this, this::render);
        viewModel.getSignedIn().observe(this, this::onSignedIn);
    }

    private void submit() {
        viewModel.createAccount(new SignUpForm(binding.fullName.getText(), binding.mobile.getText(),
                binding.email.getText(), binding.password.getInput().getText().toString(),
                binding.confirmPassword.getInput().getText().toString(), binding.acceptTerms.isChecked()));
    }

    private void render(@NonNull SignUpFormState state) {
        boolean idle = !state.loading();
        fields.forEach((field, view) -> {
            view.setError(resolve(state.errorFor(field)));
            view.setEnabled(idle);
        });
        UiText terms = state.errorFor(SignUpField.TERMS);
        binding.termsError.setText(resolve(terms));
        binding.termsError.setVisibility(terms == null ? android.view.View.GONE : android.view.View.VISIBLE);
        showBanner(binding.errorBanner, state.banner());
        binding.createAccount.setEnabled(idle);
        binding.createAccount.setText(idle ? R.string.create_account : R.string.creating_account);
        binding.google.setEnabled(idle);
        binding.acceptTerms.setEnabled(idle);
    }
}
