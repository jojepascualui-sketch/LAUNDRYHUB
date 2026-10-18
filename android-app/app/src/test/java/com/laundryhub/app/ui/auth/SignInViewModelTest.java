package com.laundryhub.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.ui.common.UiText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class SignInViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private FakeAuthRepository repo;
    private SignInViewModel vm;

    @Before
    public void setUp() {
        repo = new FakeAuthRepository();
        vm = new SignInViewModel(repo);
    }

    @Test
    public void emptyFieldsAreNotSent() {
        vm.signIn("", "", false);

        assertEquals(0, repo.calls);
        assertEquals(UiText.of(R.string.error_required), vm.getForm().getValue().identifierError());
        assertEquals(UiText.of(R.string.error_required), vm.getForm().getValue().passwordError());
    }

    @Test
    public void signInSendsTheTabRoleAndRoutesByRole() {
        vm.selectTab(SignInTab.RIDER);
        vm.signIn("0917 123 4567", "secret-pass", true);

        assertTrue(vm.getForm().getValue().loading());
        assertEquals("09171234567", repo.lastIdentifier);
        assertEquals(Role.RIDER, repo.lastTab);
        assertTrue(repo.lastRemember);

        vm.signIn("0917 123 4567", "secret-pass", true); // double tap while loading
        assertEquals(1, repo.calls);

        repo.pending.onSuccess(new Session("jwt", Long.MAX_VALUE, Role.RIDER, "Rider One"));
        assertFalse(vm.getForm().getValue().loading());
        assertEquals(Role.RIDER, vm.getSignedIn().getValue().take());
        assertNull(vm.getSignedIn().getValue().take());
    }

    @Test
    public void serverMessagesAreShown() {
        vm.signIn("ana@example.com", "wrong-pass", false);
        repo.pending.onError(new ApiException(401, "Wrong email or password.", null));
        assertEquals(UiText.of("Wrong email or password."), vm.getForm().getValue().banner());

        vm.signIn("ana@example.com", "secret-pass", false);
        repo.pending.onError(new ApiException(400, "Some fields are not valid.", Map.of("email", "must be valid")));
        assertEquals(UiText.of("must be valid"), vm.getForm().getValue().identifierError());
        assertNull(vm.getForm().getValue().banner());

        vm.signIn("ana@example.com", "secret-pass", false);
        repo.pending.onError(ApiException.network(new IOException("offline")));
        assertEquals(UiText.of(R.string.error_network), vm.getForm().getValue().banner());
    }

    @Test
    public void switchingTabsClearsErrors() {
        vm.signIn("", "", false);
        vm.selectTab(SignInTab.ADMIN);

        assertEquals(SignInTab.ADMIN, vm.getTab().getValue());
        assertNull(vm.getForm().getValue().identifierError());
    }
}
