package com.laundryhub.app.ui;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.laundryhub.app.R;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.ui.home.HomeActivity;
import com.laundryhub.app.ui.rider.RiderHomeActivity;

/**
 * Opens the first screen for a role after sign-in: customers → Home, riders → Rider home,
 * admin/staff → a message to use the desktop admin console.
 */
public final class HomeNavigator {

    private HomeNavigator() {
    }

    /**
     * @param from the sign-in or sign-up screen
     * @param role the signed-in role
     */
    public static void open(@NonNull Activity from, @NonNull Role role) {
        // OOP: Polymorphism — Role.home() is answered by each role constant.
        switch (role.home()) {
            case CUSTOMER_HOME -> start(from, HomeActivity.class);
            case RIDER_HOME -> start(from, RiderHomeActivity.class);
            case ADMIN_CONSOLE -> new MaterialAlertDialogBuilder(from)
                    .setTitle(R.string.admin_console_title)
                    .setMessage(R.string.admin_console_message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    }

    private static void start(Activity from, Class<? extends Activity> screen) {
        Intent intent = new Intent(from, screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        from.finish();
    }
}
