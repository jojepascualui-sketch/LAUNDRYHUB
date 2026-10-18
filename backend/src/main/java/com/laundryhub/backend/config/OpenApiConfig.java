package com.laundryhub.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at {@code /swagger-ui}. Sign in with {@code POST /api/auth/login}, then press
 * "Authorize" and paste the token.
 */
@Configuration
public class OpenApiConfig {

    /** Name of the bearer security scheme in the OpenAPI document. */
    public static final String BEARER_SCHEME = "bearer-jwt";

    /** @return the API description with JWT bearer auth */
    @Bean
    public OpenAPI laundryHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Laundry HUB API")
                        .version("v1")
                        .description("REST API for the Laundry HUB customer app, rider app and admin console. "
                                + "The server calculates every price; client totals are estimates only. "
                                + "Screen-to-endpoint map: docs/SCREEN_API_MAP.md."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
