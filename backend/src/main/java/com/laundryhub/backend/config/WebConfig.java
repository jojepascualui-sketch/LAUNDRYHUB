package com.laundryhub.backend.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.laundryhub.backend.security.CurrentUserIdResolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link CurrentUserIdResolver} with Spring MVC and the CORS rules that let the
 * Laundry HUB website (Vercel) call {@code /api/**} from the browser.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** How long a browser may cache a CORS preflight answer. */
    private static final Duration PREFLIGHT_CACHE = Duration.ofHours(1);

    private static final List<String> METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final List<String> allowedOrigins;

    /**
     * @param allowedOrigins comma-separated website origins ({@code WEB_ORIGINS}); patterns such as
     *                       {@code https://*.vercel.app} are allowed for preview deployments
     */
    public WebConfig(@Value("${laundryhub.web.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdResolver());
    }

    /**
     * CORS for the website. The API uses bearer tokens, not cookies, so credentials stay off.
     *
     * @return the CORS rules Spring Security applies before authentication
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(allowedOrigins);
        cors.setAllowedMethods(METHODS);
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        cors.setMaxAge(PREFLIGHT_CACHE);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
