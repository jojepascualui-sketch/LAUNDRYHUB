package com.laundryhub.backend.security;

import java.util.List;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.exception.ServiceUnavailableException;
import com.laundryhub.backend.exception.UnauthorizedException;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies Google ID tokens against Google's public keys, without any Google client library.
 * The audience must be the OAuth client id in {@code GOOGLE_OAUTH_CLIENT_ID}.
 */
// OOP: Inheritance — implements the GoogleTokenVerifier contract.
@Component
public class GoogleIdTokenVerifier implements GoogleTokenVerifier {

    /** Google's signing keys for ID tokens. */
    public static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final List<String> GOOGLE_ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";
    private static final String EMAIL_VERIFIED_CLAIM = "email_verified";

    // OOP: Encapsulation — the decoder is built once and only used inside verify().
    private final String clientId;
    private final JwtDecoder decoder;

    /**
     * @param properties holds the Google OAuth client id (blank = Google sign-in disabled)
     */
    public GoogleIdTokenVerifier(LaundryHubProperties properties) {
        this.clientId = properties.security().googleClientId();
        this.decoder = isConfigured() ? buildDecoder(clientId) : null;
    }

    private static JwtDecoder buildDecoder(String audience) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(String.valueOf(jwt.getClaims().get("iss")))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not issued by Google", null));
        OAuth2TokenValidator<Jwt> aud = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), issuer, aud));
        return nimbus;
    }

    private boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    // OOP: Polymorphism — the Google-backed implementation of verify().
    @Override
    public GoogleIdentity verify(String idToken) {
        if (!isConfigured()) {
            throw new ServiceUnavailableException("Google sign-in is not set up on the server (GOOGLE_OAUTH_CLIENT_ID)");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            String email = jwt.getClaimAsString(EMAIL_CLAIM);
            if (email == null) {
                throw new UnauthorizedException("Google account has no e-mail address");
            }
            return new GoogleIdentity(email, jwt.getClaimAsString(NAME_CLAIM),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM)));
        } catch (JwtException e) {
            throw new UnauthorizedException("Google sign-in failed. Please try again.");
        }
    }
}
