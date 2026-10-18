package com.laundryhub.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laundryhub.backend.security.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Stateless JWT security. Customers use {@code /api/orders/**}, riders {@code /api/rider/**},
 * admins and staff {@code /api/admin/**}; admin-only actions add {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Shortest HS256 key accepted (256 bits). */
    public static final int MIN_SECRET_BYTES = 32;

    private static final String ROLE_PREFIX = "ROLE_";
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * The HTTP security rules.
     *
     * @param http         Spring's builder
     * @param objectMapper writes the 401/403 problem bodies
     * @return the filter chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/google")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ping", "/api/pricing", "/api/shop").permitAll()
                        .requestMatchers("/swagger-ui", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs",
                                "/v3/api-docs/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/orders/**", "/api/geocode/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/rider/**").hasRole("RIDER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter()))
                        .authenticationEntryPoint((req, res, e) ->
                                writeProblem(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "Please sign in."))
                        .accessDeniedHandler((req, res, e) ->
                                writeProblem(objectMapper, req, res, HttpStatus.FORBIDDEN, "You do not have access to this.")))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeProblem(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "Please sign in."))
                        .accessDeniedHandler((req, res, e) ->
                                writeProblem(objectMapper, req, res, HttpStatus.FORBIDDEN, "You do not have access to this.")));
        return http.build();
    }

    private static void writeProblem(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                     HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }

    /** Turns the {@code role} claim into the single authority {@code ROLE_<role>}. */
    private static Converter<Jwt, AbstractAuthenticationToken> roleConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString(TokenService.ROLE_CLAIM);
            List<SimpleGrantedAuthority> authorities = role == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    /**
     * The HS256 signing key from {@code LAUNDRYHUB_JWT_SECRET}. If it is blank, a random key is
     * generated, so tokens stop working after a restart (fine for development only).
     *
     * @param properties application settings
     * @return the key
     */
    @Bean
    public SecretKey jwtSigningKey(LaundryHubProperties properties) {
        String secret = properties.security().jwtSecret();
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            LOG.warn("LAUNDRYHUB_JWT_SECRET is not set: using a random key, tokens will not survive a restart");
            bytes = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("LAUNDRYHUB_JWT_SECRET must be at least " + MIN_SECRET_BYTES
                        + " characters");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * @param key the HS256 key
     * @return the encoder that signs tokens
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * @param key the HS256 key
     * @return the decoder that checks incoming bearer tokens (signature and expiry)
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
