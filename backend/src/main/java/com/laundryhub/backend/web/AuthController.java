package com.laundryhub.backend.web;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.AuthService;
import com.laundryhub.backend.web.dto.AuthResponse;
import com.laundryhub.backend.web.dto.GoogleLoginRequest;
import com.laundryhub.backend.web.dto.LoginRequest;
import com.laundryhub.backend.web.dto.RegisterRequest;
import com.laundryhub.backend.web.dto.UserProfileResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Screens 00a, 00b, 00c: sign in, sign up and Google sign-in. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "00a Sign in · 00b Create account · 00c Admin sign in")
public class AuthController {

    private final AuthService auth;

    /**
     * @param auth sign-in logic
     */
    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /**
     * @param request sign-up form
     * @return token and profile of the new customer
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "00b Create account (customer)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    /**
     * @param request e-mail, password and the selected tab
     * @return token and profile; the role decides the next screen
     */
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "00a/00c Sign in (Customer / Rider / Admin tab)")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    /**
     * @param request Google ID token
     * @return token and profile
     */
    @PostMapping("/google")
    @SecurityRequirements
    @Operation(summary = "00a Continue with Google (customers only)")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return auth.google(request);
    }

    /**
     * @param userId the signed-in user
     * @return their profile ("Hi, [RIDER NAME]", admin sidebar)
     */
    @GetMapping("/me")
    @Operation(summary = "Signed-in account")
    public UserProfileResponse me(@CurrentUserId Long userId) {
        return auth.me(userId);
    }
}
