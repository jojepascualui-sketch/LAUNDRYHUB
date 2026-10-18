package com.laundryhub.app.ui.rider;

import com.laundryhub.app.R;
import com.laundryhub.app.ui.home.PlaceholderHomeActivity;

/** Rider landing screen (placeholder for R1-rider-bookings). */
// OOP: Inheritance — a PlaceholderHomeActivity.
public class RiderHomeActivity extends PlaceholderHomeActivity {

    // OOP: Polymorphism — rider message.
    @Override
    protected int messageRes() {
        return R.string.rider_home_placeholder;
    }
}
