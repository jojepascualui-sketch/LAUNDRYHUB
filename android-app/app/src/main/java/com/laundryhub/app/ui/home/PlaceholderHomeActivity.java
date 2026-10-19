package com.laundryhub.app.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.R;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.databinding.ActivityPlaceholderHomeBinding;
import com.laundryhub.app.ui.auth.SignInActivity;
import com.laundryhub.app.ui.common.SystemBars;

/**
 * Temporary landing screen after sign-in, until R1 Rider bookings is built:
 * shows who is signed in and a Sign out button.
 */
// OOP: Abstraction — subclasses only say which message to show.
// OOP: Inheritance — RiderHomeActivity extends this class (customers now have HomeActivity).
public abstract class PlaceholderHomeActivity extends AppCompatActivity {

    /** @return the explanation shown under the title */
    // OOP: Polymorphism — each home screen supplies its own message.
    @StringRes
    protected abstract int messageRes();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForLightScreen(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        Session session = app.sessions().getActive();
        if (session == null) {
            backToSignIn();
            return;
        }
        ActivityPlaceholderHomeBinding binding = ActivityPlaceholderHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.padForSystemBars(binding.root);
        binding.title.setText(getString(R.string.home_placeholder_title, session.getFullName()));
        binding.message.setText(messageRes());
        binding.signOut.setOnClickListener(v -> {
            app.authRepository().signOut();
            backToSignIn();
        });
    }

    private void backToSignIn() {
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
