package com.laundryhub.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.laundryhub.app.R;
import com.laundryhub.app.model.Role;

import org.junit.Test;

/** The tabs must change text and behavior exactly like source/Auth.dc.html. */
public class SignInTabTest {

    @Test
    public void tabsAreInDesignOrder() {
        assertEquals(SignInTab.CUSTOMER, SignInTab.atIndex(0));
        assertEquals(SignInTab.RIDER, SignInTab.atIndex(1));
        assertEquals(SignInTab.ADMIN, SignInTab.atIndex(2));
    }

    @Test
    public void headingsLabelsAndPlaceholdersFollowTheTab() {
        assertEquals(R.string.sign_in_heading_customer, SignInTab.CUSTOMER.getHeading());
        assertEquals(R.string.label_email, SignInTab.CUSTOMER.getEmailLabel());
        assertEquals(R.string.sign_in_heading_rider, SignInTab.RIDER.getHeading());
        assertEquals(R.string.label_rider_email, SignInTab.RIDER.getEmailLabel());
        assertEquals(R.string.hint_email, SignInTab.RIDER.getEmailHint());
        assertEquals(R.string.sign_in_heading_admin, SignInTab.ADMIN.getHeading());
        assertEquals(R.string.label_staff_email, SignInTab.ADMIN.getEmailLabel());
        assertEquals(R.string.hint_staff_email, SignInTab.ADMIN.getEmailHint());
    }

    @Test
    public void googleIsForCustomersOnly() {
        assertTrue(SignInTab.CUSTOMER.showsGoogle());
        assertFalse(SignInTab.RIDER.showsGoogle());
        assertFalse(SignInTab.ADMIN.showsGoogle());
    }

    @Test
    public void onlyRidersMaySignInWithAMobileNumber() {
        assertNull(SignInTab.RIDER.validateIdentifier("0917 123 4567"));
        assertEquals("09171234567", SignInTab.RIDER.normalizeIdentifier("0917 123 4567"));
        assertNull(SignInTab.RIDER.validateIdentifier("rider@example.com"));
        assertEquals(Integer.valueOf(R.string.error_email), SignInTab.CUSTOMER.validateIdentifier("09171234567"));
        assertEquals(Integer.valueOf(R.string.error_required), SignInTab.ADMIN.validateIdentifier(""));
    }

    @Test
    public void tabsSendTheirRole() {
        assertEquals(Role.CUSTOMER, SignInTab.CUSTOMER.getRole());
        assertEquals(Role.RIDER, SignInTab.RIDER.getRole());
        assertEquals(Role.ADMIN, SignInTab.ADMIN.getRole());
    }
}
