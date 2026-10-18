package com.laundryhub.backend.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.laundryhub.backend.security.GoogleTokenVerifier.GoogleIdentity;

import org.junit.jupiter.api.Test;

/** 00a / 00b / 00c and role-based access. */
class AuthApiTest extends ApiTestBase {

    private Map<String, Object> signUp(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Juan Dela Cruz");
        body.put("mobileNumber", "09181234567");
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("confirmPassword", PASSWORD);
        body.put("acceptedTerms", true);
        return body;
    }

    @Test
    void createAccountReturnsACustomerToken() throws Exception {
        postJson("/api/auth/register", null, signUp("juan@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.initials").value("JD"));

        postJson("/api/auth/register", null, signUp("JUAN@example.com")).andExpect(status().isConflict());
    }

    @Test
    void createAccountValidatesTheForm() throws Exception {
        Map<String, Object> body = signUp("juan@example.com");
        body.put("confirmPassword", "different-pass");
        body.put("mobileNumber", "12345");
        body.put("acceptedTerms", false);

        postJson("/api/auth/register", null, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mobileNumber").value("must look like 09XXXXXXXXX"))
                .andExpect(jsonPath("$.errors.acceptedTerms").exists())
                .andExpect(jsonPath("$.errors.passwordConfirmed").value("Passwords do not match"));
    }

    @Test
    void signInChecksPasswordAndTab() throws Exception {
        rider("Rider One", "rider1@example.com", true);
        staff();

        postJson("/api/auth/login", null, Map.of("email", "rider1@example.com", "password", PASSWORD, "role", "RIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("RIDER"))
                .andExpect(jsonPath("$.user.plateNumber").isNotEmpty());
        postJson("/api/auth/login", null, Map.of("email", "rider1@example.com", "password", "wrong-pass"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."));
        postJson("/api/auth/login", null,
                Map.of("email", "rider1@example.com", "password", PASSWORD, "role", "CUSTOMER"))
                .andExpect(status().isForbidden());
        // The Admin tab also lets staff in; both use the admin console.
        postJson("/api/auth/login", null,
                Map.of("email", "counter@laundryhub.test", "password", PASSWORD, "role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STAFF"))
                .andExpect(jsonPath("$.user.usesAdminConsole").value(true));
    }

    @Test
    void ridersMaySignInWithTheirMobileNumber() throws Exception {
        rider("Rider One", "rider1@example.com", true); // mobile 09180000002

        postJson("/api/auth/login", null, Map.of("email", "09180000002", "password", PASSWORD, "role", "RIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("rider1@example.com"));
        // Only on the Rider tab.
        postJson("/api/auth/login", null, Map.of("email", "09180000002", "password", PASSWORD, "role", "CUSTOMER"))
                .andExpect(status().isUnauthorized());
        // Two riders with the same number: ambiguous, so no sign-in by number.
        rider("Rider Two", "rider2@example.com", true);
        postJson("/api/auth/login", null, Map.of("email", "09180000002", "password", PASSWORD, "role", "RIDER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleSignInCreatesACustomerOnce() throws Exception {
        when(googleVerifier.verify(anyString()))
                .thenReturn(new GoogleIdentity("maria@gmail.com", "Maria Santos", true));

        postJson("/api/auth/google", null, Map.of("idToken", "google-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.fullName").value("Maria Santos"));
        postJson("/api/auth/google", null, Map.of("idToken", "google-token")).andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(users.findByEmailIgnoreCase("maria@gmail.com")).isPresent();
        org.assertj.core.api.Assertions.assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void googleSignInIsForCustomersOnly() throws Exception {
        rider("Rider One", "rider1@example.com", true);
        when(googleVerifier.verify(anyString()))
                .thenReturn(new GoogleIdentity("rider1@example.com", "Rider One", true));

        postJson("/api/auth/google", null, Map.of("idToken", "google-token")).andExpect(status().isForbidden());
    }

    @Test
    void rolesProtectEachApp() throws Exception {
        String customer = customer("ana@example.com");
        String rider = rider("Rider One", "rider1@example.com", true);
        String staff = staff();

        getJson("/api/auth/me", customer).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CUSTOMER"));
        getJson("/api/orders/mine", null).andExpect(status().isUnauthorized());
        getJson("/api/orders/mine", "not-a-jwt").andExpect(status().isUnauthorized());
        getJson("/api/orders/mine", rider).andExpect(status().isForbidden());
        getJson("/api/rider/summary", customer).andExpect(status().isForbidden());
        getJson("/api/admin/dashboard", customer).andExpect(status().isForbidden());
        getJson("/api/admin/dashboard", staff).andExpect(status().isOk());
        // Staff run orders but may not change prices or accounts.
        getJson("/api/admin/pricing", staff).andExpect(status().isForbidden());
        getJson("/api/admin/users", staff).andExpect(status().isForbidden());
        getJson("/api/admin/reports", staff).andExpect(status().isForbidden());
    }

    @Test
    void deactivatedAccountsLoseAccessImmediately() throws Exception {
        String customer = customer("ana@example.com");
        jdbc.update("UPDATE users SET status = 'INACTIVE'");

        getJson("/api/orders/mine", customer).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", null, Map.of("email", "ana@example.com", "password", PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocsArePublic() throws Exception {
        getJson("/v3/api-docs", null).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Laundry HUB API"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
