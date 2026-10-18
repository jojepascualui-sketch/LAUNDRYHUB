package com.laundryhub.backend.service;

import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.user.AccountStatus;
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
import com.laundryhub.backend.web.dto.AuthResponse;
import com.laundryhub.backend.web.dto.GoogleLoginRequest;
import com.laundryhub.backend.web.dto.LoginRequest;
import com.laundryhub.backend.web.dto.RegisterRequest;
import com.laundryhub.backend.web.dto.UserProfileResponse;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign-up (00b), sign-in (00a/00c) and "Continue with Google". Every result carries a JWT with the role. */
@Service
public class AuthService {

    private static final String WRONG_CREDENTIALS = "Wrong email or password.";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final GoogleTokenVerifier googleVerifier;
    private final AccountLookup accounts;

    /**
     * @param users           account repository
     * @param passwordEncoder BCrypt
     * @param tokens          issues JWTs
     * @param googleVerifier  checks Google ID tokens
     * @param accounts        loads the signed-in account
     */
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                       GoogleTokenVerifier googleVerifier, AccountLookup accounts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.googleVerifier = googleVerifier;
        this.accounts = accounts;
    }

    /**
     * 00b "Create account": always a customer account.
     *
     * @param request sign-up form
     * @return token and profile
     * @throws ConflictException if the e-mail is already used
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("An account with this email already exists. Sign in instead.");
        }
        Customer customer = new Customer(request.fullName(), request.email(), request.mobileNumber());
        customer.changePasswordHash(passwordEncoder.encode(request.password()));
        return issueFor(users.save(customer));
    }

    /**
     * 00a/00c "Sign in". The selected tab must match the account: Customer, Rider, or Admin
     * (which also accepts staff, since both use the admin console).
     *
     * @param request e-mail, password and tab
     * @return token and profile
     * @throws UnauthorizedException if the credentials are wrong or the account is inactive
     * @throws ForbiddenException    if the account does not belong to the selected tab
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = findAccount(request.email().trim(), request.role())
                .orElseThrow(() -> new UnauthorizedException(WRONG_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(WRONG_CREDENTIALS);
        }
        if (!user.canSignIn()) {
            throw new UnauthorizedException("This account has been deactivated.");
        }
        if (request.role() != null && !belongsToTab(user, request.role())) {
            throw new ForbiddenException("This is not a " + request.role().name().toLowerCase()
                    + " account. Choose the right tab.");
        }
        return issueFor(user);
    }

    /**
     * E-mail for every tab; the Rider tab also accepts the rider's mobile number
     * ("Rider email or mobile"), if exactly one rider has it.
     */
    private Optional<User> findAccount(String identifier, Role tab) {
        if (tab == Role.RIDER && Check.MOBILE_NUMBER.matcher(identifier).matches()) {
            List<User> riders = users.findByMobileNumber(identifier).stream()
                    .filter(u -> u.getRole() == Role.RIDER)
                    .toList();
            return riders.size() == 1 ? Optional.of(riders.get(0)) : Optional.empty();
        }
        return users.findByEmailIgnoreCase(identifier);
    }

    // OOP: Polymorphism — canUseAdminConsole() is answered by the User subclass.
    private static boolean belongsToTab(User user, Role tab) {
        return tab == Role.ADMIN || tab == Role.STAFF ? user.canUseAdminConsole() : user.getRole() == tab;
    }

    /**
     * 00a "Continue with Google" (customers only). Creates the customer account on first use.
     *
     * @param request the Google ID token
     * @return token and profile
     * @throws UnauthorizedException if the token is invalid or the e-mail is not verified
     * @throws ForbiddenException    if the e-mail belongs to a rider, admin or staff account
     */
    @Transactional
    public AuthResponse google(GoogleLoginRequest request) {
        GoogleIdentity identity = googleVerifier.verify(request.idToken());
        if (!identity.emailVerified()) {
            throw new UnauthorizedException("Your Google e-mail address is not verified.");
        }
        User user = users.findByEmailIgnoreCase(identity.email()).orElse(null);
        if (user == null) {
            String name = identity.fullName() == null || identity.fullName().isBlank()
                    ? identity.email().substring(0, identity.email().indexOf('@'))
                    : identity.fullName();
            user = users.save(new Customer(name, identity.email(), null));
        } else if (user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Google sign-in is for customers only. Use your email and password.");
        } else if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("This account has been deactivated.");
        }
        return issueFor(user);
    }

    /**
     * @param userId the signed-in user
     * @return their profile
     */
    @Transactional(readOnly = true)
    public UserProfileResponse me(Long userId) {
        return UserProfileResponse.from(accounts.anyRole(userId));
    }

    private AuthResponse issueFor(User user) {
        IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), AuthResponse.BEARER, token.expiresAt(), UserProfileResponse.from(user));
    }
}
