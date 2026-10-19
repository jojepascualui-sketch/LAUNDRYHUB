package com.laundryhub.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.ForbiddenException;
import com.laundryhub.backend.exception.UnauthorizedException;
import com.laundryhub.backend.repository.UserRepository;
import com.laundryhub.backend.security.GoogleTokenVerifier;
import com.laundryhub.backend.security.GoogleTokenVerifier.GoogleIdentity;
import com.laundryhub.backend.security.TokenService;
import com.laundryhub.backend.security.TokenService.IssuedToken;
import com.laundryhub.backend.testsupport.Fixtures;
import com.laundryhub.backend.web.dto.AuthResponse;
import com.laundryhub.backend.web.dto.GoogleLoginRequest;
import com.laundryhub.backend.web.dto.LoginRequest;
import com.laundryhub.backend.web.dto.RegisterRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** AuthService: 00a sign in per tab, 00b create account and Google sign-in. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int FAST_BCRYPT_COST = 4;
    private static final String PASSWORD = "secret-pass-1";
    private static final String WRONG = "Wrong email or password.";
    private static final String TOKEN = "signed-token";

    @Mock
    private UserRepository users;
    @Mock
    private TokenService tokens;
    @Mock
    private GoogleTokenVerifier google;
    @Mock
    private AccountLookup accounts;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(FAST_BCRYPT_COST);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, tokens, google, accounts);
        lenient().when(tokens.issue(any())).thenReturn(new IssuedToken(TOKEN, Instant.now()));
        lenient().when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private <T extends User> T stored(T user) {
        user.changePasswordHash(encoder.encode(PASSWORD));
        lenient().when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    private static LoginRequest login(String email, String password, Role tab) {
        return new LoginRequest(email, password, tab);
    }

    @Test
    void rightPasswordOnTheRightTabIssuesAToken() {
        Customer ana = stored(Fixtures.customer());

        AuthResponse response = service.login(login(" ana@example.com ", PASSWORD, Role.CUSTOMER));

        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(response.tokenType()).isEqualTo(AuthResponse.BEARER);
        assertThat(response.user().role()).isEqualTo(Role.CUSTOMER);
        verify(tokens).issue(ana);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
        stored(Fixtures.customer());

        assertThatThrownBy(() -> service.login(login("ana@example.com", "wrong-pass", null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(WRONG);
        assertThatThrownBy(() -> service.login(login("nobody@example.com", PASSWORD, null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(WRONG);
        verify(tokens, never()).issue(any());
    }

    @Test
    void accountWithoutAPasswordCannotUseThePasswordForm() {
        Customer googleOnly = Fixtures.customer();
        when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(googleOnly));

        assertThatThrownBy(() -> service.login(login("ana@example.com", PASSWORD, null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(WRONG);
    }

    @Test
    void wrongTabIsForbidden() {
        stored(Fixtures.rider("rider1@example.com", true));
        stored(Fixtures.customer());

        assertThatThrownBy(() -> service.login(login("rider1@example.com", PASSWORD, Role.CUSTOMER)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This is not a customer account. Choose the right tab.");
        assertThatThrownBy(() -> service.login(login("ana@example.com", PASSWORD, Role.ADMIN)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.login(login("ana@example.com", PASSWORD, Role.RIDER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminTabAdmitsBothConsoleRoles() {
        stored(Fixtures.admin());
        stored(Fixtures.staff());

        assertThat(service.login(login("owner@laundryhub.test", PASSWORD, Role.ADMIN)).user().role())
                .isEqualTo(Role.ADMIN);
        assertThat(service.login(login("counter@laundryhub.test", PASSWORD, Role.ADMIN)).user().usesAdminConsole())
                .isTrue();
        assertThat(service.login(login("owner@laundryhub.test", PASSWORD, Role.STAFF)).user().role())
                .isEqualTo(Role.ADMIN);
        assertThatThrownBy(() -> service.login(login("owner@laundryhub.test", PASSWORD, Role.CUSTOMER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivatedAccountCannotSignIn() {
        Customer ana = stored(Fixtures.customer());
        ana.deactivate();

        assertThatThrownBy(() -> service.login(login("ana@example.com", PASSWORD, Role.CUSTOMER)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("This account has been deactivated.");
    }

    @Test
    void ridersSignInWithAUniqueMobileNumberOnTheRiderTabOnly() {
        var rider = stored(Fixtures.rider("rider1@example.com", true));
        when(users.findByMobileNumber("09180000002")).thenReturn(List.<User>of(rider));

        assertThat(service.login(login("09180000002", PASSWORD, Role.RIDER)).user().email())
                .isEqualTo("rider1@example.com");

        when(users.findByMobileNumber("09180000002"))
                .thenReturn(List.<User>of(rider, Fixtures.rider("rider2@example.com", true)));
        assertThatThrownBy(() -> service.login(login("09180000002", PASSWORD, Role.RIDER)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void registerRefusesADuplicateEmail() {
        when(users.existsByEmailIgnoreCase("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(register(" juan@example.com ")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account with this email already exists. Sign in instead.");
        verify(users, never()).save(any());
    }

    @Test
    void registerStoresACustomerWithAHashedPassword() {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);

        AuthResponse response = service.register(register("juan@example.com"));

        verify(users).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(Customer.class);
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.user().initials()).isEqualTo("JD");
    }

    private static RegisterRequest register(String email) {
        return new RegisterRequest("Juan Dela Cruz", "09181234567", email, PASSWORD, PASSWORD, true);
    }

    @Test
    void googleSignInNeedsAVerifiedEmail() {
        when(google.verify("t")).thenReturn(new GoogleIdentity("maria@gmail.com", "Maria", false));

        assertThatThrownBy(() -> service.google(new GoogleLoginRequest("t")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your Google e-mail address is not verified.");
    }

    @Test
    void googleSignInCreatesACustomerNamedAfterTheEmailWhenNoNameIsGiven() {
        when(google.verify("t")).thenReturn(new GoogleIdentity("maria@gmail.com", " ", true));
        when(users.findByEmailIgnoreCase("maria@gmail.com")).thenReturn(Optional.empty());

        AuthResponse response = service.google(new GoogleLoginRequest("t"));

        assertThat(response.user().fullName()).isEqualTo("maria");
        assertThat(response.user().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void googleSignInIsForActiveCustomersOnly() {
        when(google.verify("staff")).thenReturn(new GoogleIdentity("counter@laundryhub.test", "Staff", true));
        when(users.findByEmailIgnoreCase("counter@laundryhub.test")).thenReturn(Optional.of(Fixtures.staff()));
        Customer inactive = Fixtures.customer();
        inactive.deactivate();
        when(google.verify("ana")).thenReturn(new GoogleIdentity("ana@example.com", "Ana", true));
        when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.google(new GoogleLoginRequest("staff")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.google(new GoogleLoginRequest("ana")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("This account has been deactivated.");
    }
}
