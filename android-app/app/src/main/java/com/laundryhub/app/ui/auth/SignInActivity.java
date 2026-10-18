package com.laundryhub.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ActivitySignInBinding;
import com.laundryhub.app.ui.common.ViewModelFactory;

/** 00a-sign-in and 00c-admin-sign-in (Admin tab). */
// OOP: Inheritance — an AuthActivity with the sign-in form.
public class SignInActivity extends AuthActivity {

    private ActivitySignInBinding binding;
    private SignInViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppContainer app = LaundryHubApp.container(this);
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(SignInViewModel.class,
                () -> new SignInViewModel(app.authRepository()))).get(SignInViewModel.class);
        applyInsets(binding.header, binding.form);

        binding.roleTabs.setOnSegmentSelectedListener(i -> viewModel.selectTab(SignInTab.atIndex(i)));
        binding.signIn.setOnClickListener(v -> submit());
        binding.password.getInput().setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        // No design for the reset flow yet (CLAUDE.md §7).
        binding.forgotPassword.setOnClickListener(v -> showMessage(R.string.forgot_password_unavailable));
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
        binding.goToSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        viewModel.getTab().observe(this, this::renderTab);
        viewModel.getForm().observe(this, this::renderForm);
        viewModel.getSignedIn().observe(this, this::onSignedIn);
    }

    private void submit() {
        viewModel.signIn(binding.email.getText(), binding.password.getInput().getText().toString(),
                binding.rememberMe.isChecked());
    }

    /** Heading, subheading, email label and placeholder, and the Google button follow the tab. */
    private void renderTab(@NonNull SignInTab tab) {
        binding.roleTabs.setSelectedIndex(tab.ordinal());
        setHeader(binding.header, tab.getHeading(), tab.getSubheading());
        binding.email.setLabel(tab.getEmailLabel());
        binding.email.setHint(tab.getEmailHint());
        // OOP: Polymorphism — each SignInTab decides whether Google sign-in is offered.
        int google = tab.showsGoogle() ? View.VISIBLE : View.GONE;
        binding.orDivider.getRoot().setVisibility(google);
        binding.google.setVisibility(google);
    }

    private void renderForm(@NonNull SignInFormState state) {
        boolean idle = !state.loading();
        binding.email.setError(resolve(state.identifierError()));
        binding.password.setError(resolve(state.passwordError()));
        showBanner(binding.errorBanner, state.banner());
        binding.signIn.setEnabled(idle);
        binding.signIn.setText(idle ? R.string.sign_in : R.string.signing_in);
        binding.google.setEnabled(idle);
        binding.email.setEnabled(idle);
        binding.password.setEnabled(idle);
        binding.roleTabs.setEnabled(idle);
    }
}
