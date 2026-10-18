package com.laundryhub.app.ui.home;

import com.laundryhub.app.R;

/** Customer landing screen (placeholder for 01-home). */
// OOP: Inheritance — a PlaceholderHomeActivity.
public class CustomerHomeActivity extends PlaceholderHomeActivity {

    // OOP: Polymorphism — customer message.
    @Override
    protected int messageRes() {
        return R.string.customer_home_placeholder;
    }
}
