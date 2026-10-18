package com.laundryhub.backend.security;

import java.time.Duration;
import java.time.Instant;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.user.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Signs HS256 JWTs whose subject is the user id and whose {@code role} claim drives access rules. */
// OOP: Inheritance — implements the TokenService contract.
@Service
public class JwtTokenService implements TokenService {

    /** Value of the {@code iss} claim. */
    public static final String ISSUER = "laundryhub";

    // OOP: Encapsulation — the encoder and lifetime are fixed at construction.
    private final JwtEncoder encoder;
    private final Duration lifetime;

    /**
     * @param encoder    the HS256 encoder from {@link SecurityConfig}
     * @param properties token lifetime
     */
    public JwtTokenService(JwtEncoder encoder, LaundryHubProperties properties) {
        this.encoder = Check.notNull(encoder, "JWT encoder");
        this.lifetime = Duration.ofHours(properties.security().tokenHours());
    }

    // OOP: Polymorphism — the JWT implementation of TokenService.issue.
    @Override
    public IssuedToken issue(User user) {
        Check.notNull(user, "User");
        Check.notNull(user.getId(), "User id");
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(NAME_CLAIM, user.getFullName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
