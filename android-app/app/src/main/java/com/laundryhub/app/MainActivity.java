package com.laundryhub.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.ui.HomeNavigator;
import com.laundryhub.app.ui.auth.SignInActivity;

/**
 * Launcher: opens the signed-in role's home when a remembered, unexpired session exists,
 * otherwise Sign in (00a).
 */
// OOP: Inheritance — a screen-less AppCompatActivity that only routes.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = LaundryHubApp.container(this).sessions().getActive();
        if (session != null && session.getRole().usesMobileApp()) {
            HomeNavigator.open(this, session.getRole());
        } else {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
    }
}
